package com.umatgong.domain.place.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umatgong.domain.place.dto.PlaceResponse;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.place.repository.KakaoPlaceUpsertRepository;
import com.umatgong.domain.place.repository.PlaceRepository;
import com.umatgong.global.geo.Coordinate;
import com.umatgong.global.kakao.KakaoCategory;
import com.umatgong.global.kakao.KakaoLocalClient;
import com.umatgong.global.kakao.KakaoPlace;

/**
 * 카카오 장소 검색 + 캐싱.
 *
 * <p>카카오 로컬 API에는 "장소 ID로 조회"가 없어서, 검색 전에 external_id로 미리 찾을 수가 없다.
 * 그래서 두 단계로 호출을 줄인다.
 * <ol>
 *   <li>검색 결과 캐시 — 같은 검색(좌표·반경·키워드)이면 카카오를 부르지 않고 저장해 둔 장소 ID 목록을 쓴다</li>
 *   <li>places upsert — 카카오 결과는 external_id 기준으로 한 행에 모인다. 같은 가게가 여러 검색에 나와도 한 번만 저장된다</li>
 * </ol>
 * 캐시에는 장소 ID만 두고 장소 정보는 항상 DB에서 읽는다.
 */
@Service
public class PlaceService {

	// 소수점 넷째 자리(약 11m)로 반올림해 캐시 키를 만든다. 지도를 조금 움직여도 같은 검색으로 본다.
	private static final String COORDINATE_KEY_FORMAT = "%.4f,%.4f";
	private static final Duration CACHE_TTL = Duration.ofHours(1);
	private static final long CACHE_MAX_ENTRIES = 10_000;

	private final KakaoLocalClient kakaoLocalClient;
	private final PlaceRepository placeRepository;
	private final KakaoPlaceUpsertRepository upsertRepository;
	private final TransactionTemplate writeTransaction;
	private final TransactionTemplate readTransaction;
	private final Cache<String, List<String>> searchCache = Caffeine.newBuilder()
		.expireAfterWrite(CACHE_TTL)
		.maximumSize(CACHE_MAX_ENTRIES)
		.build();

	public PlaceService(KakaoLocalClient kakaoLocalClient, PlaceRepository placeRepository,
		KakaoPlaceUpsertRepository upsertRepository, PlatformTransactionManager transactionManager) {
		this.kakaoLocalClient = kakaoLocalClient;
		this.placeRepository = placeRepository;
		this.upsertRepository = upsertRepository;
		this.writeTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction.setReadOnly(true);
	}

	/** 반경 안의 음식점·카페. 가까운 순. */
	public List<PlaceResponse> nearby(Coordinate center, int radiusMeters) {
		String key = "nearby:" + coordinateKey(center) + ":" + radiusMeters;
		// 같은 키로 동시에 들어오면 한 요청만 카카오를 부르고 나머지는 그 결과를 기다린다.
		List<String> externalIds = searchCache.get(key, k -> fetchNearby(center, radiusMeters));
		return load(externalIds, center);
	}

	/**
	 * 가게명 등 키워드 검색. 음식점·카페만 돌려준다. 카카오의 정확도 순서를 유지한다.
	 *
	 * @param center 거리 표시용 기준 좌표. 없으면 null. 검색 결과 자체는 바꾸지 않으므로 캐시 키에 넣지 않는다
	 */
	public List<PlaceResponse> search(String query, Coordinate center) {
		String normalized = query.strip();
		String key = "keyword:" + normalized.toLowerCase(Locale.ROOT);
		List<String> externalIds = searchCache.get(key, k -> fetchKeyword(normalized, center));
		return load(externalIds, center);
	}

	private List<String> fetchNearby(Coordinate center, int radiusMeters) {
		List<KakaoPlace> merged = new ArrayList<>();
		for (KakaoCategory category : KakaoCategory.values()) {
			merged.addAll(kakaoLocalClient.searchByCategory(category, center, radiusMeters));
		}
		List<KakaoPlace> sorted = distinctByExternalId(merged).stream()
			.sorted(Comparator.comparingDouble(p -> center.distanceMetersTo(p.coordinate())))
			.toList();
		return save(sorted);
	}

	private List<String> fetchKeyword(String query, Coordinate center) {
		List<KakaoPlace> places = kakaoLocalClient.searchByKeyword(query, center).stream()
			.filter(p -> KakaoCategory.isSupported(p.categoryGroupCode()))
			.toList();
		return save(distinctByExternalId(places));
	}

	// 카카오 호출은 트랜잭션 밖에서 끝내고, 저장만 짧은 트랜잭션으로 한다.
	private List<String> save(List<KakaoPlace> places) {
		if (!places.isEmpty()) {
			writeTransaction.executeWithoutResult(status -> upsertRepository.upsertAll(places));
		}
		return places.stream().map(KakaoPlace::externalId).toList();
	}

	private List<PlaceResponse> load(List<String> externalIds, Coordinate center) {
		if (externalIds.isEmpty()) {
			return List.of();
		}
		Map<String, Place> byExternalId = readTransaction.execute(status ->
			placeRepository.findAllByExternalIdIn(externalIds).stream()
				.collect(Collectors.toMap(Place::getExternalId, Function.identity())));
		// 캐시에 담긴 순서(가까운 순 / 정확도 순)를 그대로 지킨다.
		return externalIds.stream()
			.map(byExternalId::get)
			.filter(Objects::nonNull)
			.map(place -> PlaceResponse.of(place, center))
			.toList();
	}

	private static List<KakaoPlace> distinctByExternalId(List<KakaoPlace> places) {
		Map<String, KakaoPlace> unique = new LinkedHashMap<>();
		places.forEach(p -> unique.putIfAbsent(p.externalId(), p));
		return List.copyOf(unique.values());
	}

	private static String coordinateKey(Coordinate c) {
		return String.format(Locale.ROOT, COORDINATE_KEY_FORMAT, c.lat(), c.lng());
	}
}

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
import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubMemberId;
import com.umatgong.domain.club.repository.ClubMemberRepository;
import com.umatgong.domain.club.repository.ClubRepository;
import com.umatgong.domain.place.dto.CreateCustomPlaceRequest;
import com.umatgong.domain.place.dto.PlaceResponse;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.place.repository.KakaoPlaceUpsertRepository;
import com.umatgong.domain.place.repository.PlaceRepository;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
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
 *
 * <p><b>클럽 전용 장소</b>(「여기 없어요」로 직접 입력, source=USER)는 카카오 결과에 섞어 돌려준다.
 * 요청자가 <b>지금</b> 속한 클럽의 것만 섞는다. 캐시에 넣지 않고 요청마다 DB에서 읽는다 — 클럽마다 결과가 달라서다.
 */
@Service
public class PlaceService {

	// 소수점 넷째 자리(약 11m)로 반올림해 캐시 키를 만든다. 지도를 조금 움직여도 같은 검색으로 본다.
	private static final String COORDINATE_KEY_FORMAT = "%.4f,%.4f";
	private static final Duration CACHE_TTL = Duration.ofHours(1);
	private static final long CACHE_MAX_ENTRIES = 10_000;
	// 키워드 검색에 섞는 클럽 전용 장소 수. 카카오 한 페이지(15)와 같다
	private static final int MAX_CUSTOM_SEARCH = 15;
	// 같은 클럽에 같은 이름의 가게가 이만큼 가까이 있으면 새로 만들지 않고 그 장소를 쓴다
	private static final double DUPLICATE_RADIUS_METERS = 50;

	private final KakaoLocalClient kakaoLocalClient;
	private final PlaceRepository placeRepository;
	private final KakaoPlaceUpsertRepository upsertRepository;
	private final ClubMemberRepository clubMemberRepository;
	private final ClubRepository clubRepository;
	private final UserRepository userRepository;
	private final TransactionTemplate writeTransaction;
	private final TransactionTemplate readTransaction;
	private final Cache<String, List<String>> searchCache = Caffeine.newBuilder()
		.expireAfterWrite(CACHE_TTL)
		.maximumSize(CACHE_MAX_ENTRIES)
		.build();

	public PlaceService(KakaoLocalClient kakaoLocalClient, PlaceRepository placeRepository,
		KakaoPlaceUpsertRepository upsertRepository, ClubMemberRepository clubMemberRepository,
		ClubRepository clubRepository, UserRepository userRepository, PlatformTransactionManager transactionManager) {
		this.kakaoLocalClient = kakaoLocalClient;
		this.placeRepository = placeRepository;
		this.upsertRepository = upsertRepository;
		this.clubMemberRepository = clubMemberRepository;
		this.clubRepository = clubRepository;
		this.userRepository = userRepository;
		this.writeTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction.setReadOnly(true);
	}

	/** 반경 안의 음식점·카페와 내 클럽 전용 장소. 가까운 순. */
	public List<PlaceResponse> nearby(Long viewerId, Coordinate center, int radiusMeters) {
		String key = "nearby:" + coordinateKey(center) + ":" + radiusMeters;
		// 같은 키로 동시에 들어오면 한 요청만 카카오를 부르고 나머지는 그 결과를 기다린다.
		List<String> externalIds = searchCache.get(key, k -> fetchNearby(center, radiusMeters));
		List<PlaceResponse> merged = new ArrayList<>(load(externalIds, center));
		merged.addAll(customWithin(viewerId, center, radiusMeters));
		merged.sort(Comparator.comparing(PlaceResponse::distanceMeters));
		return merged;
	}

	/**
	 * 가게명 등 키워드 검색. 음식점·카페만 돌려준다. 카카오의 정확도 순서를 유지한다.
	 *
	 * @param center 거리 표시용 기준 좌표. 없으면 null. 검색 결과 자체는 바꾸지 않으므로 캐시 키에 넣지 않는다
	 */
	public List<PlaceResponse> search(Long viewerId, String query, Coordinate center) {
		String normalized = query.strip();
		String key = "keyword:" + normalized.toLowerCase(Locale.ROOT);
		List<String> externalIds = searchCache.get(key, k -> fetchKeyword(normalized, center));
		// 우리 클럽이 직접 등록한 가게가 먼저다. 카카오에 없어서 등록한 곳이므로 찾는 사람이 바로 그곳을 찾는 경우가 많다
		List<PlaceResponse> merged = new ArrayList<>(customByName(viewerId, normalized, center));
		merged.addAll(load(externalIds, center));
		return merged;
	}

	/**
	 * 클럽 전용 장소를 만든다. 내가 멤버인 클럽에만. 멤버가 아니면 클럽이 있는지도 알려주지 않는다.
	 * 같은 클럽에 이름이 같고(대소문자·앞뒤 공백 무시) 50m 안인 장소가 있으면 새로 만들지 않고 그것을 돌려준다.
	 */
	public PlaceResponse createCustom(Long viewerId, CreateCustomPlaceRequest request) {
		return writeTransaction.execute(status -> {
			if (!clubMemberRepository.existsById(new ClubMemberId(request.clubId(), viewerId))) {
				throw new BusinessException(ErrorCode.CLUB_NOT_FOUND);
			}
			String name = request.name().strip();
			Coordinate coordinate = Coordinate.of(request.coordinate().lat(), request.coordinate().lng());
			Place existing = placeRepository.findCustomWithin(List.of(request.clubId()), coordinate.lat(),
					coordinate.lng(), DUPLICATE_RADIUS_METERS).stream()
				.filter(place -> place.getName().strip().equalsIgnoreCase(name))
				.findFirst()
				.orElse(null);
			if (existing != null) {
				return PlaceResponse.of(existing, null);
			}
			Club club = clubRepository.getReferenceById(request.clubId());
			User user = userRepository.getReferenceById(viewerId);
			String address = request.address() == null || request.address().isBlank() ? null : request.address().strip();
			return PlaceResponse.of(placeRepository.save(Place.custom(club, name, address, coordinate, user)), null);
		});
	}

	private List<PlaceResponse> customWithin(Long viewerId, Coordinate center, int radiusMeters) {
		return readTransaction.execute(status -> {
			List<Long> clubIds = clubMemberRepository.findClubIdsByUserId(viewerId);
			if (clubIds.isEmpty()) {
				return List.<PlaceResponse>of();
			}
			return placeRepository.findCustomWithin(clubIds, center.lat(), center.lng(), radiusMeters).stream()
				.map(place -> PlaceResponse.of(place, center))
				.toList();
		});
	}

	private List<PlaceResponse> customByName(Long viewerId, String query, Coordinate center) {
		return readTransaction.execute(status -> {
			List<Long> clubIds = clubMemberRepository.findClubIdsByUserId(viewerId);
			if (clubIds.isEmpty()) {
				return List.<PlaceResponse>of();
			}
			String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
			return placeRepository.findCustomByName(clubIds, pattern, MAX_CUSTOM_SEARCH).stream()
				.map(place -> PlaceResponse.of(place, center))
				.toList();
		});
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

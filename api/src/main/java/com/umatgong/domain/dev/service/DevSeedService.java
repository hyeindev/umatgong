package com.umatgong.domain.dev.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubMember;
import com.umatgong.domain.club.repository.ClubMemberRepository;
import com.umatgong.domain.club.repository.ClubRepository;
import com.umatgong.domain.club.service.ClubColorPicker;
import com.umatgong.domain.club.service.InviteCodeGenerator;
import com.umatgong.domain.dev.dto.DevSeedResponse;
import com.umatgong.domain.dev.service.SeedVisitPlanner.PlannedVisit;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.place.repository.KakaoPlaceUpsertRepository;
import com.umatgong.domain.place.repository.PlaceRepository;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.domain.visit.entity.Visibility;
import com.umatgong.domain.visit.entity.Visit;
import com.umatgong.domain.visit.repository.VisitRepository;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.geo.Coordinate;
import com.umatgong.global.kakao.KakaoCategory;
import com.umatgong.global.kakao.KakaoLocalClient;
import com.umatgong.global.kakao.KakaoPlace;

import lombok.extern.slf4j.Slf4j;

/**
 * 개발용 시드. 로그인한 사용자에게 수원 광교 일대 실제 음식점과 방문 기록이 담긴 클럽을 하나 만들어 준다.
 *
 * <p><b>멱등:</b> 시드 클럽은 “내가 클럽장인, 이름이 {@value #CLUB_NAME}인 클럽”으로 찾는다.
 * 이미 기록이 있으면 카카오도 부르지 않고 그대로 돌려준다. 저장은 사용자 행을 잠근 채 다시 확인하므로
 * 동시에 여러 번 불러도 클럽·기록이 두 벌 생기지 않는다. 기록은 (시드 클럽, 장소)마다 하나다.
 *
 * <p>요금제의 “한 사람당 클럽 수” 제한은 적용하지 않는다. 이미 다른 클럽에 있어도 시드 클럽이 하나 더 생긴다.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "umatgong.dev.seed", name = "enabled", havingValue = "true")
public class DevSeedService {

	static final String CLUB_NAME = "광교 맛집 (시드)";
	static final int MAX_PLACES = 20;
	// 광교중앙역과 상현역. 두 곳을 반경 안에서 음식점 검색해 광교 일대에 고르게 퍼지게 한다.
	static final List<Coordinate> CENTERS = List.of(
		Coordinate.of(37.28866, 127.05170),
		Coordinate.of(37.29758, 127.06915));
	static final int RADIUS_METERS = 1000;

	private final KakaoLocalClient kakaoLocalClient;
	private final KakaoPlaceUpsertRepository upsertRepository;
	private final PlaceRepository placeRepository;
	private final ClubRepository clubRepository;
	private final ClubMemberRepository clubMemberRepository;
	private final UserRepository userRepository;
	private final VisitRepository visitRepository;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final Clock clock;
	private final TransactionTemplate transaction;

	public DevSeedService(KakaoLocalClient kakaoLocalClient, KakaoPlaceUpsertRepository upsertRepository,
		PlaceRepository placeRepository, ClubRepository clubRepository, ClubMemberRepository clubMemberRepository,
		UserRepository userRepository, VisitRepository visitRepository, InviteCodeGenerator inviteCodeGenerator,
		Clock clock, PlatformTransactionManager transactionManager) {
		this.kakaoLocalClient = kakaoLocalClient;
		this.upsertRepository = upsertRepository;
		this.placeRepository = placeRepository;
		this.clubRepository = clubRepository;
		this.clubMemberRepository = clubMemberRepository;
		this.userRepository = userRepository;
		this.visitRepository = visitRepository;
		this.inviteCodeGenerator = inviteCodeGenerator;
		this.clock = clock;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	public DevSeedResponse seed(Long userId) {
		Optional<DevSeedResponse> existing = transaction.execute(status -> findSeeded(userId));
		if (existing.isPresent()) {
			return existing.get();
		}
		// 카카오 호출은 트랜잭션 밖에서 끝내고, 저장만 트랜잭션으로 한다 (PlaceService와 같은 방식).
		List<KakaoPlace> places = fetchPlaces();
		if (places.isEmpty()) {
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE, "광교 일대 음식점을 찾지 못했습니다.");
		}
		return transaction.execute(status -> save(userId, places));
	}

	private Optional<DevSeedResponse> findSeeded(Long userId) {
		return clubRepository.findFirstByCreatedByIdAndNameOrderByIdAsc(userId, CLUB_NAME)
			.map(club -> summary(club, visitRepository.findPlaceIdsByUserIdAndClubId(userId, club.getId()), false))
			.filter(summary -> summary.visitCount() > 0);
	}

	private List<KakaoPlace> fetchPlaces() {
		// 두 검색 결과를 번갈아 섞어서, 앞에서부터 잘라도 두 동네가 고르게 들어가게 한다.
		List<List<KakaoPlace>> results = CENTERS.stream()
			.map(center -> kakaoLocalClient.searchByCategory(KakaoCategory.RESTAURANT, center, RADIUS_METERS))
			.toList();
		Map<String, KakaoPlace> unique = new LinkedHashMap<>();
		int longest = results.stream().mapToInt(List::size).max().orElse(0);
		for (int i = 0; i < longest; i++) {
			for (List<KakaoPlace> result : results) {
				if (i < result.size()) {
					unique.putIfAbsent(result.get(i).externalId(), result.get(i));
				}
			}
		}
		return unique.values().stream()
			.filter(p -> KakaoCategory.RESTAURANT.code().equals(p.categoryGroupCode()))
			.limit(MAX_PLACES)
			.toList();
	}

	private DevSeedResponse save(Long userId, List<KakaoPlace> kakaoPlaces) {
		// 같은 사용자의 시드·클럽 생성이 동시에 오면 여기서 줄을 선다. 잠근 뒤 다시 찾는다.
		User user = userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		boolean clubCreated = false;
		Club club = clubRepository.findFirstByCreatedByIdAndNameOrderByIdAsc(userId, CLUB_NAME).orElse(null);
		if (club == null) {
			club = clubRepository.save(Club.create(CLUB_NAME,
				ClubColorPicker.pick(clubMemberRepository.findClubColorsByUserId(userId)), user,
				inviteCodeGenerator.next()));
			clubMemberRepository.save(ClubMember.join(club, user));
			clubCreated = true;
		}

		upsertRepository.upsertAll(kakaoPlaces);
		Map<String, Place> byExternalId = placeRepository.findAllByExternalIdIn(
				kakaoPlaces.stream().map(KakaoPlace::externalId).toList()).stream()
			.collect(Collectors.toMap(Place::getExternalId, Function.identity()));
		List<Place> places = kakaoPlaces.stream().map(p -> byExternalId.get(p.externalId())).toList();

		Set<Long> visitedPlaceIds = new HashSet<>(visitRepository.findPlaceIdsByUserIdAndClubId(userId, club.getId()));
		List<PlannedVisit> plan = SeedVisitPlanner.plan(places.size(), clock.instant(), userId);
		List<Visit> visits = new ArrayList<>();
		for (int i = 0; i < places.size(); i++) {
			Place place = places.get(i);
			if (visitedPlaceIds.add(place.getId())) {
				PlannedVisit planned = plan.get(i);
				visits.add(Visit.record(user, place, club, planned.rating(), planned.memo(), planned.visitedAt(),
					Visibility.CLUB));
			}
		}
		visitRepository.saveAll(visits);
		log.info("dev seed user={} club={} -> {} places, {} new visits", userId, club.getId(), places.size(),
			visits.size());
		return summary(club, visitRepository.findPlaceIdsByUserIdAndClubId(userId, club.getId()),
			clubCreated || !visits.isEmpty());
	}

	private static DevSeedResponse summary(Club club, List<Long> visitedPlaceIds, boolean created) {
		int placeCount = new HashSet<>(visitedPlaceIds).size();
		return new DevSeedResponse(club.getId(), club.getName(), placeCount, visitedPlaceIds.size(), created);
	}
}

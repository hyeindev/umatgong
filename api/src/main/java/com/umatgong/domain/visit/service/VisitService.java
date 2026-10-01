package com.umatgong.domain.visit.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubMemberId;
import com.umatgong.domain.club.repository.ClubMemberRepository;
import com.umatgong.domain.club.repository.ClubRepository;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.place.repository.PlaceRepository;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.domain.visit.dto.CreateVisitRequest;
import com.umatgong.domain.visit.dto.UpdateVisitRequest;
import com.umatgong.domain.visit.dto.VisitPageResponse;
import com.umatgong.domain.visit.dto.VisitPinResponse;
import com.umatgong.domain.visit.dto.VisitResponse;
import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.entity.Visibility;
import com.umatgong.domain.visit.entity.Visit;
import com.umatgong.domain.visit.entity.VisitPhoto;
import com.umatgong.domain.visit.repository.VisitPhotoRepository;
import com.umatgong.domain.visit.repository.VisitQueryRepository;
import com.umatgong.domain.visit.repository.VisitRepository;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.geo.Coordinate;

import lombok.RequiredArgsConstructor;

/**
 * 방문 기록.
 *
 * <p><b>권한은 전부 여기서 정한다.</b> 조회는 요청 때마다 요청자의 현재 클럽 목록을 DB에서 다시 읽어
 * {@link VisitQueryRepository}의 클럽 경계 조건에 넘긴다. 토큰이나 요청 값에 담긴 클럽 정보는 믿지 않는다.
 * 그래서 클럽을 탈퇴하면 다음 요청부터 그 클럽 기록이 바로 안 보인다.
 */
@Service
@RequiredArgsConstructor
public class VisitService {

	static final int MAX_PINS = 500;
	static final int MAX_NEARBY = 100;
	static final int MAX_PER_PLACE = 100;
	static final int DEFAULT_PAGE_SIZE = 30;
	static final int MAX_PAGE_SIZE = 100;
	// 기기 시계가 조금 빠른 경우는 받아 준다. 그 이상 미래는 잘못된 값이다.
	private static final Duration FUTURE_TOLERANCE = Duration.ofDays(1);

	private final VisitRepository visitRepository;
	private final VisitPhotoRepository visitPhotoRepository;
	private final VisitQueryRepository visitQueryRepository;
	private final PlaceRepository placeRepository;
	private final ClubRepository clubRepository;
	private final ClubMemberRepository clubMemberRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Transactional
	public VisitResponse create(Long userId, CreateVisitRequest request) {
		if (request.visitedAt().isAfter(clock.instant().plus(FUTURE_TOLERANCE))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "visitedAt이 미래입니다.");
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

		Club club = null;
		if (request.clubId() != null) {
			// 기록을 남길 클럽의 멤버인지 확인한다. 멤버가 아니면 클럽이 있는지도 알려주지 않는다.
			if (!clubMemberRepository.existsById(new ClubMemberId(request.clubId(), userId))) {
				throw new BusinessException(ErrorCode.CLUB_NOT_FOUND);
			}
			club = clubRepository.getReferenceById(request.clubId());
		}

		Place place = placeRepository.findById(request.placeId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
		// 커스텀 장소는 만든 클럽 안에서만 쓴다. 다른 클럽의 커스텀 장소는 없는 장소처럼 다룬다.
		if (place.isCustom() && (club == null || !place.getClub().getId().equals(club.getId()))) {
			throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
		}

		Visibility visibility = request.visibility() != null
			? request.visibility()
			: club != null ? Visibility.CLUB : Visibility.PRIVATE;
		if (visibility == Visibility.CLUB && club == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "클럽 공개 기록에는 clubId가 필요합니다.");
		}

		Visit visit = visitRepository.save(Visit.record(user, place, club, request.rating(),
			blankToNull(request.memo()), request.visitedAt(), visibility));
		if (request.thumbnailUrl() != null) {
			visitPhotoRepository.save(VisitPhoto.attach(visit, request.thumbnailUrl(), null, null));
		}
		visitRepository.flush();
		return visible(userId, visit.getId());
	}

	@Transactional(readOnly = true)
	public List<VisitPinResponse> pins(Long userId, Coordinate sw, Coordinate ne, Collection<Long> filterClubIds) {
		return visitQueryRepository.pins(userId, myClubIds(userId), sw, ne, filterClubIds, MAX_PINS);
	}

	@Transactional(readOnly = true)
	public List<VisitResponse> nearby(Long userId, Coordinate center, int radiusMeters, Collection<Rating> ratings) {
		return visitQueryRepository.nearby(userId, myClubIds(userId), center, radiusMeters, ratings, MAX_NEARBY);
	}

	/**
	 * 한 장소의 기록. 보이는 기록이 없으면 빈 목록이다.
	 * 다른 클럽의 커스텀 장소는 장소가 있다는 사실도 알려주지 않는다.
	 */
	@Transactional(readOnly = true)
	public List<VisitResponse> byPlace(Long userId, Long placeId) {
		List<Long> clubIds = myClubIds(userId);
		Place place = placeRepository.findById(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
		if (place.isCustom() && !clubIds.contains(place.getClub().getId())) {
			throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
		}
		return visitQueryRepository.byPlace(userId, clubIds, placeId, MAX_PER_PLACE);
	}

	@Transactional(readOnly = true)
	public VisitPageResponse mine(Long userId, String cursor, Integer size) {
		int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		VisitCursor after = cursor == null || cursor.isBlank() ? null : VisitCursor.decode(cursor);
		// 다음 페이지가 있는지 알려고 한 건 더 읽는다
		List<VisitResponse> rows = visitQueryRepository.mine(userId, myClubIds(userId),
			after == null ? null : after.visitedAt(), after == null ? null : after.id(), pageSize + 1);
		if (rows.size() <= pageSize) {
			return new VisitPageResponse(rows, null);
		}
		List<VisitResponse> page = rows.subList(0, pageSize);
		VisitResponse last = page.get(page.size() - 1);
		return new VisitPageResponse(List.copyOf(page), new VisitCursor(last.visitedAt(), last.id()).encode());
	}

	/** 평가·메모 수정. 내게 보이는 내 기록만 고칠 수 있다 */
	@Transactional
	public VisitResponse update(Long userId, Long visitId, UpdateVisitRequest request) {
		requireVisible(userId, visitId);
		Visit visit = requireOwn(userId, visitId);
		if (request.rating() != null) {
			visit.changeRating(request.rating());
		}
		if (request.memo() != null) {
			visit.editMemo(blankToNull(request.memo()));
		}
		visitRepository.flush();
		return visible(userId, visitId);
	}

	/**
	 * 내 기록 삭제. 탈퇴한 클럽에 남긴 내 기록도 지울 수 있다 (기획서 C-04 “내 기록 삭제”).
	 * 남의 기록은 보이면 FORBIDDEN, 보이지 않으면 없는 기록과 같이 VISIT_NOT_FOUND다.
	 */
	@Transactional
	public void delete(Long userId, Long visitId) {
		Visit visit = visitRepository.findById(visitId)
			.orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
		if (!visit.isWrittenBy(userId)) {
			requireVisible(userId, visitId);
			throw new BusinessException(ErrorCode.FORBIDDEN, "내 기록만 지울 수 있습니다.");
		}
		visitRepository.delete(visit);
	}

	private Visit requireOwn(Long userId, Long visitId) {
		Visit visit = visitRepository.findById(visitId)
			.orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
		if (!visit.isWrittenBy(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "내 기록만 고칠 수 있습니다.");
		}
		return visit;
	}

	private void requireVisible(Long userId, Long visitId) {
		visible(userId, visitId);
	}

	private VisitResponse visible(Long userId, Long visitId) {
		return visitQueryRepository.findVisible(userId, myClubIds(userId), visitId)
			.orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
	}

	private List<Long> myClubIds(Long userId) {
		return clubMemberRepository.findClubIdsByUserId(userId);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}

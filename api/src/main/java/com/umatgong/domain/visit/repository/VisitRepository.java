package com.umatgong.domain.visit.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.visit.entity.Visibility;
import com.umatgong.domain.visit.entity.Visit;

public interface VisitRepository extends JpaRepository<Visit, Long> {

	/** 이 사용자가 이 클럽에 남긴 기록의 장소 ID (기록 하나당 하나). 개발용 시드의 중복 방지에 쓴다 */
	@Query("select v.place.id from Visit v where v.user.id = :userId and v.club.id = :clubId")
	List<Long> findPlaceIdsByUserIdAndClubId(@Param("userId") Long userId, @Param("clubId") Long clubId);

	/**
	 * 클럽별 공개(CLUB) 기록 수. 클럽 목록에서 클럽마다 따로 세지 않도록 한 번에 센다.
	 * 비공개 기록은 쓴 사람 말고는 볼 수 없으므로 세지 않는다 — 숫자로 존재가 새지 않게 한다.
	 * 호출하는 쪽이 요청자가 멤버인 클럽 ID만 넘긴다.
	 */
	@Query("select v.club.id as clubId, count(v) as visitCount from Visit v "
		+ "where v.club.id in :clubIds and v.visibility = :visibility group by v.club.id")
	List<ClubVisitCount> countByClubIds(@Param("clubIds") Collection<Long> clubIds,
		@Param("visibility") Visibility visibility);

	/**
	 * 한 클럽의 멤버별 기록 수. 요청자에게 보이는 기록만 센다 — 공개 기록, 그리고 요청자 본인의 비공개 기록.
	 * 지도·목록에서 보이는 것과 같은 기준이다 (VisitQueryRepository.VISIBLE).
	 */
	@Query("select v.user.id as userId, count(v) as visitCount from Visit v "
		+ "where v.club.id = :clubId and (v.visibility = :visibility or v.user.id = :viewerId) "
		+ "group by v.user.id")
	List<MemberVisitCount> countVisibleByMember(@Param("clubId") Long clubId, @Param("viewerId") Long viewerId,
		@Param("visibility") Visibility visibility);

	interface ClubVisitCount {
		Long getClubId();

		long getVisitCount();
	}

	interface MemberVisitCount {
		Long getUserId();

		long getVisitCount();
	}
}

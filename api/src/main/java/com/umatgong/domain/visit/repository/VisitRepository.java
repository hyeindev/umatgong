package com.umatgong.domain.visit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.visit.entity.Visit;

public interface VisitRepository extends JpaRepository<Visit, Long> {

	/** 이 사용자가 이 클럽에 남긴 기록의 장소 ID (기록 하나당 하나). 개발용 시드의 중복 방지에 쓴다 */
	@Query("select v.place.id from Visit v where v.user.id = :userId and v.club.id = :clubId")
	List<Long> findPlaceIdsByUserIdAndClubId(@Param("userId") Long userId, @Param("clubId") Long clubId);
}

package com.umatgong.domain.club.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.club.entity.Club;

import jakarta.persistence.LockModeType;

public interface ClubRepository extends JpaRepository<Club, Long> {

	// 정원 확인과 합류를 한 덩어리로 하기 위해 클럽 행을 잠근다.
	// 잠그지 않으면 마지막 한 자리에 두 명이 동시에 들어와 정원을 넘는다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Club c where c.inviteCode = :inviteCode")
	Optional<Club> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Club c where c.id = :id")
	Optional<Club> findByIdForUpdate(@Param("id") Long id);

	/** 개발용 시드 클럽 찾기. 클럽장이 탈퇴하면 created_by가 비므로, 찾았다면 그 사용자는 아직 멤버다 */
	Optional<Club> findFirstByCreatedByIdAndNameOrderByIdAsc(Long userId, String name);
}

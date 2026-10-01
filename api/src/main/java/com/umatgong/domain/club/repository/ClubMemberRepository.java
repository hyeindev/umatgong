package com.umatgong.domain.club.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.club.entity.ClubMember;
import com.umatgong.domain.club.entity.ClubMemberId;

public interface ClubMemberRepository extends JpaRepository<ClubMember, ClubMemberId> {

	long countByClubId(Long clubId);

	long countByUserId(Long userId);

	/** 내가 속한 클럽. 클럽을 함께 읽어 N+1을 막는다 */
	@Query("select m from ClubMember m join fetch m.club where m.user.id = :userId order by m.joinedAt")
	List<ClubMember> findWithClubByUserId(@Param("userId") Long userId);

	/** 클럽 멤버. 사용자를 함께 읽어 N+1을 막는다 */
	@Query("select m from ClubMember m join fetch m.user where m.club.id = :clubId order by m.joinedAt")
	List<ClubMember> findWithUserByClubId(@Param("clubId") Long clubId);

	/** 클럽별 멤버 수. 목록 화면에서 클럽마다 count를 따로 날리지 않기 위해 한 번에 센다 */
	@Query("select m.club.id as clubId, count(m) as memberCount from ClubMember m "
		+ "where m.club.id in :clubIds group by m.club.id")
	List<MemberCount> countByClubIds(@Param("clubIds") List<Long> clubIds);

	/** 지금 속한 클럽 ID. 기록 조회의 클럽 경계는 이 목록으로만 정한다 */
	@Query("select m.club.id from ClubMember m where m.user.id = :userId")
	List<Long> findClubIdsByUserId(@Param("userId") Long userId);

	@Query("select m.club.color from ClubMember m where m.user.id = :userId")
	List<ClubColor> findClubColorsByUserId(@Param("userId") Long userId);

	interface MemberCount {
		Long getClubId();

		long getMemberCount();
	}
}

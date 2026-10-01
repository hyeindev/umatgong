package com.umatgong.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umatgong.domain.user.entity.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

	// 이메일 동의를 받지 않으므로 사용자는 카카오 회원번호로만 찾는다.
	Optional<User> findByKakaoId(Long kakaoId);

	// 한 사람이 속할 수 있는 클럽 수를 확인하고 클럽을 만들거나 합류하는 동안 사용자 행을 잠근다.
	// 잠그지 않으면 같은 사용자가 동시에 두 클럽에 들어가 제한을 넘는다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(@Param("id") Long id);
}

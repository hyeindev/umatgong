package com.umatgong.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umatgong.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	// 이메일 동의를 받지 않으므로 사용자는 카카오 회원번호로만 찾는다.
	Optional<User> findByKakaoId(Long kakaoId);
}

package com.umatgong.domain.user.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private Long kakaoId;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 500)
	private String avatarUrl;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private User(Long kakaoId, String name, String avatarUrl) {
		this.kakaoId = kakaoId;
		this.name = name;
		this.avatarUrl = avatarUrl;
	}

	public static User signUpWithKakao(Long kakaoId, String name, String avatarUrl) {
		return new User(kakaoId, name, avatarUrl);
	}

	public void updateProfile(String name, String avatarUrl) {
		this.name = name;
		this.avatarUrl = avatarUrl;
	}
}

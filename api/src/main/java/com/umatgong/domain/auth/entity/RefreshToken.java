package com.umatgong.domain.auth.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.umatgong.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회전 방식 리프레시 토큰. 갱신할 때마다 새 토큰을 발급하고 이전 것은 폐기한다.
 * 한 번의 로그인에서 이어진 토큰들은 같은 familyId를 가진다. 이미 폐기된 토큰이 다시 쓰이면
 * 탈취로 보고 서비스가 그 family 전체를 폐기한다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// 원문이 아니라 SHA-256 hex. DB가 유출돼도 바로 쓸 수 없게.
	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(nullable = false)
	private UUID familyId;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt) {
		this.user = Objects.requireNonNull(user, "user");
		this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
		this.familyId = Objects.requireNonNull(familyId, "familyId");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
	}

	/** 로그인 시 새 family를 시작한다. */
	public static RefreshToken issueOnLogin(User user, String tokenHash, Instant expiresAt) {
		return new RefreshToken(user, tokenHash, UUID.randomUUID(), expiresAt);
	}

	/**
	 * 이 토큰을 폐기하고 같은 family의 다음 토큰을 돌려준다.
	 *
	 * @throws IllegalStateException 이미 폐기됐거나 만료된 토큰. 폐기된 토큰이면 재사용(탈취) 신호다.
	 */
	public RefreshToken rotate(String nextTokenHash, Instant nextExpiresAt, Instant now) {
		if (!isActive(now)) {
			throw new IllegalStateException("Refresh token is not active");
		}
		revoke(now);
		return new RefreshToken(user, nextTokenHash, familyId, nextExpiresAt);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && now.isBefore(expiresAt);
	}
}

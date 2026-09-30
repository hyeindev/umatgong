package com.umatgong.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.umatgong.domain.user.entity.User;

class RefreshTokenTest {

	private final User user = User.signUpWithKakao(1L, "지현", null);
	private final Instant now = Instant.parse("2026-09-30T00:00:00Z");

	@Test
	void 회전하면_이전_토큰은_폐기되고_같은_family의_새_토큰이_나온다() {
		RefreshToken first = RefreshToken.issueOnLogin(user, "a".repeat(64), now.plus(Duration.ofDays(14)));

		RefreshToken second = first.rotate("b".repeat(64), now.plus(Duration.ofDays(14)), now);

		assertThat(first.isRevoked()).isTrue();
		assertThat(second.isActive(now)).isTrue();
		assertThat(second.getFamilyId()).isEqualTo(first.getFamilyId());
	}

	@Test
	void 폐기된_토큰은_다시_회전할_수_없다() {
		RefreshToken first = RefreshToken.issueOnLogin(user, "a".repeat(64), now.plus(Duration.ofDays(14)));
		first.rotate("b".repeat(64), now.plus(Duration.ofDays(14)), now);

		assertThatThrownBy(() -> first.rotate("c".repeat(64), now.plus(Duration.ofDays(14)), now))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 만료된_토큰은_활성이_아니다() {
		RefreshToken token = RefreshToken.issueOnLogin(user, "a".repeat(64), now);

		assertThat(token.isActive(now)).isFalse();
	}
}

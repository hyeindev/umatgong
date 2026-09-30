package com.umatgong.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

class JwtProviderTest {

	private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";
	private static final String OTHER_SECRET = "another-secret-another-secret-9876543210abcd";

	private final JwtProvider provider = provider(SECRET, Duration.ofMinutes(30));

	@Test
	void 발급한_토큰에서_사용자_ID를_꺼낸다() {
		String token = provider.createAccessToken(42L);

		assertThat(provider.parseAccessToken(token)).isEqualTo(42L);
	}

	@Test
	void 만료_시간은_설정값을_따른다() {
		String token = provider.createAccessToken(42L);

		var claims = Jwts.parser()
			.verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).build()
			.parseSignedClaims(token).getPayload();
		long seconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
		assertThat(seconds).isEqualTo(1800);
	}

	@Test
	void 만료된_토큰은_거부한다() {
		String expired = provider(SECRET, Duration.ofSeconds(-60)).createAccessToken(42L);

		assertThatThrownBy(() -> provider.parseAccessToken(expired)).isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void 본문을_바꾼_토큰은_서명_검증에서_거부한다() {
		String[] parts = provider.createAccessToken(42L).split("\\.");
		// sub를 1로 바꾼 본문을 원래 서명과 붙인다.
		String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
			"{\"sub\":\"1\",\"typ\":\"access\",\"iat\":1,\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));
		String forged = parts[0] + "." + forgedPayload + "." + parts[2];

		assertThatThrownBy(() -> provider.parseAccessToken(forged)).isInstanceOf(JwtException.class);
	}

	@Test
	void 서명을_한_글자만_바꿔도_거부한다() {
		String token = provider.createAccessToken(42L);
		// 마지막 글자는 base64url 패딩 비트라 바꿔도 같은 바이트로 디코딩될 수 있다. 서명 가운데를 바꾼다.
		int i = token.lastIndexOf('.') + 10;
		String tampered = token.substring(0, i) + (token.charAt(i) == 'A' ? 'B' : 'A') + token.substring(i + 1);

		assertThatThrownBy(() -> provider.parseAccessToken(tampered)).isInstanceOf(JwtException.class);
	}

	@Test
	void 다른_키로_서명한_토큰은_거부한다() {
		String foreign = provider(OTHER_SECRET, Duration.ofMinutes(30)).createAccessToken(42L);

		assertThatThrownBy(() -> provider.parseAccessToken(foreign)).isInstanceOf(JwtException.class);
	}

	@Test
	void 서명_없는_토큰은_거부한다() {
		String unsigned = Jwts.builder().subject("42").claim("typ", "access")
			.expiration(new Date(System.currentTimeMillis() + 60_000)).compact();

		assertThatThrownBy(() -> provider.parseAccessToken(unsigned)).isInstanceOf(JwtException.class);
	}

	@Test
	void 같은_키로_서명했어도_액세스_토큰이_아니면_거부한다() {
		String notAccess = Jwts.builder().subject("42").claim("typ", "refresh")
			.expiration(new Date(System.currentTimeMillis() + 60_000))
			.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();

		assertThatThrownBy(() -> provider.parseAccessToken(notAccess)).isInstanceOf(JwtException.class);
	}

	@Test
	void 형식이_아닌_문자열은_거부한다() {
		assertThatThrownBy(() -> provider.parseAccessToken("not-a-jwt")).isInstanceOf(JwtException.class);
	}

	@Test
	void 시크릿이_256비트보다_짧으면_만들어지지_않는다() {
		assertThatThrownBy(() -> provider("short-secret", Duration.ofMinutes(30)))
			.isInstanceOf(WeakKeyException.class);
	}

	private static JwtProvider provider(String secret, Duration accessValidity) {
		return new JwtProvider(new JwtProperties(secret, accessValidity, Duration.ofDays(14)));
	}
}

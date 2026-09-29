package com.umatgong.global.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

	private static final String TOKEN_TYPE_CLAIM = "typ";
	private static final String ACCESS_TOKEN_TYPE = "access";

	private final SecretKey key;
	private final JwtProperties properties;

	public JwtProvider(JwtProperties properties) {
		// 256비트 미만 시크릿이면 여기서 WeakKeyException으로 기동이 실패한다. 의도된 동작.
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.properties = properties;
	}

	public String createAccessToken(Long userId) {
		Date now = new Date();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
			.issuedAt(now)
			.expiration(new Date(now.getTime() + properties.accessTokenValidity().toMillis()))
			.signWith(key)
			.compact();
	}

	/**
	 * @throws io.jsonwebtoken.ExpiredJwtException 만료된 토큰
	 * @throws JwtException 서명·형식이 잘못됐거나 액세스 토큰이 아닌 경우
	 */
	public Long parseAccessToken(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
		// 나중에 다른 종류의 토큰을 같은 키로 발급해도 액세스 토큰 자리에 쓰이지 않게 막는다.
		if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
			throw new JwtException("Not an access token");
		}
		return Long.valueOf(claims.getSubject());
	}
}

package com.umatgong.domain.dev.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 개발용 시드 API 설정. <b>운영에서는 켜지 않는다.</b>
 *
 * @param enabled true일 때만 시드 API가 등록된다 (APP_DEV_SEED_ENABLED, 기본 false)
 * @param token   요청 헤더로 받아야 하는 시크릿 (APP_DEV_SEED_TOKEN). {@value #MIN_TOKEN_LENGTH}자보다 짧으면
 *                어떤 요청도 받지 않는다
 */
@ConfigurationProperties(prefix = "umatgong.dev.seed")
public record DevSeedProperties(boolean enabled, String token) {

	public static final int MIN_TOKEN_LENGTH = 32;

	public boolean hasUsableToken() {
		return token != null && token.strip().length() >= MIN_TOKEN_LENGTH;
	}

	/** 헤더 값이 설정된 토큰과 같은지. 비교 시간으로 토큰을 추측할 수 없게 고정 시간 비교를 쓴다. */
	public boolean accepts(String presented) {
		if (!enabled || !hasUsableToken() || presented == null) {
			return false;
		}
		return MessageDigest.isEqual(token.strip().getBytes(StandardCharsets.UTF_8),
			presented.getBytes(StandardCharsets.UTF_8));
	}
}

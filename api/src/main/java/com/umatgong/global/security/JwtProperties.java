package com.umatgong.global.security;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * 만료 시간은 단위 없이 숫자만 오면 초로 읽는다. 기본 단위(밀리초)로 두면
 * 환경변수에 1800을 넣었을 때 1.8초짜리 토큰이 나간다.
 */
@ConfigurationProperties(prefix = "umatgong.jwt")
public record JwtProperties(
	String secret,
	@DurationUnit(ChronoUnit.SECONDS) Duration accessTokenValidity,
	@DurationUnit(ChronoUnit.SECONDS) Duration refreshTokenValidity
) {
}

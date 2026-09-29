package com.umatgong.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param allowCredentials 토큰을 쿠키로 주고받을 때만 true. 헤더 방식이면 false로 둔다.
 *                         true로 바꾸면 allowedOrigins에 와일드카드를 쓸 수 없고 CSRF 대책도 필요해진다.
 */
@ConfigurationProperties(prefix = "umatgong.cors")
public record CorsProperties(List<String> allowedOrigins, boolean allowCredentials) {
}

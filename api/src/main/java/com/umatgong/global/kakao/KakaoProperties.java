package com.umatgong.global.kakao;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param restApiKey          서버 전용. 로그 출력이나 응답에 절대 넣지 않는다 (toString도 가린다).
 * @param appId               카카오 개발자 콘솔의 앱 ID. 로그인 토큰이 우리 앱에서 발급된 것인지 확인하는 데 쓴다.
 * @param clientSecret        카카오 앱에서 Client Secret을 켰을 때만 둔다. 비밀값이다. 없으면 null
 * @param allowedRedirectUris 웹 로그인 인가 코드 교환에 쓸 수 있는 redirectUri. 문자열이 정확히 같아야 한다
 */
@ConfigurationProperties(prefix = "umatgong.kakao")
public record KakaoProperties(String restApiKey, Long appId, String clientSecret, List<String> allowedRedirectUris) {

	public KakaoProperties {
		clientSecret = clientSecret == null || clientSecret.isBlank() ? null : clientSecret;
		allowedRedirectUris = allowedRedirectUris == null ? List.of() : allowedRedirectUris.stream()
			.map(String::strip)
			.filter(uri -> !uri.isEmpty())
			.toList();
	}

	@Override
	public String toString() {
		return "KakaoProperties[restApiKey=****, appId=" + appId
			+ ", clientSecret=" + (clientSecret == null ? "none" : "****")
			+ ", allowedRedirectUris=" + allowedRedirectUris + "]";
	}
}

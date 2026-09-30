package com.umatgong.global.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param restApiKey 서버 전용. 로그 출력이나 응답에 절대 넣지 않는다 (toString도 가린다).
 * @param appId      카카오 개발자 콘솔의 앱 ID. 로그인 토큰이 우리 앱에서 발급된 것인지 확인하는 데 쓴다.
 */
@ConfigurationProperties(prefix = "umatgong.kakao")
public record KakaoProperties(String restApiKey, Long appId) {

	@Override
	public String toString() {
		return "KakaoProperties[restApiKey=****, appId=" + appId + "]";
	}
}

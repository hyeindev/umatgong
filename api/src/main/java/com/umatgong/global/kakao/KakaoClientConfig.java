package com.umatgong.global.kakao;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoClientConfig {

	private static final String KAPI_BASE_URL = "https://kapi.kakao.com";
	private static final String DAPI_BASE_URL = "https://dapi.kakao.com";
	private static final String KAUTH_BASE_URL = "https://kauth.kakao.com";

	/** 사용자 API (로그인 검증). KakaoAuthClient가 쓴다. */
	@Bean
	public RestClient kakaoApiRestClient(RestClient.Builder builder) {
		return builder.baseUrl(KAPI_BASE_URL).requestFactory(requestFactory()).build();
	}

	/** 인증 서버 (웹 로그인의 인가 코드 → 토큰 교환). KakaoAuthClient가 쓴다. */
	@Bean
	public RestClient kakaoOAuthRestClient(RestClient.Builder builder) {
		return builder.baseUrl(KAUTH_BASE_URL).requestFactory(requestFactory()).build();
	}

	/**
	 * 로컬 API (장소 검색). KakaoLocalClient가 쓴다.
	 * REST API 키는 여기서 기본 헤더로 넣지 않는다. 키는 요청을 만드는 클라이언트 클래스 안에서만 다룬다.
	 */
	@Bean
	public RestClient kakaoLocalRestClient(RestClient.Builder builder) {
		return builder.baseUrl(DAPI_BASE_URL).requestFactory(requestFactory()).build();
	}

	// 카카오가 느려도 요청 스레드가 오래 묶이지 않게 타임아웃을 짧게 둔다.
	private static ClientHttpRequestFactory requestFactory() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(2))
			.withReadTimeout(Duration.ofSeconds(3));
		return ClientHttpRequestFactoryBuilder.detect().build(settings);
	}
}

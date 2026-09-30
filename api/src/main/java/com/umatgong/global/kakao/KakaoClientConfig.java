package com.umatgong.global.kakao;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoClientConfig {

	private static final String KAPI_BASE_URL = "https://kapi.kakao.com";

	// 카카오가 느려도 로그인 요청 스레드가 오래 묶이지 않게 타임아웃을 짧게 둔다.
	@Bean
	public RestClient kakaoApiRestClient(RestClient.Builder builder) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(2))
			.withReadTimeout(Duration.ofSeconds(3));
		return builder
			.baseUrl(KAPI_BASE_URL)
			.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
			.build();
	}
}

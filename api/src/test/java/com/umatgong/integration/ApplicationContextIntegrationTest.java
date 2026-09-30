package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.umatgong.domain.auth.controller.AuthController;
import com.umatgong.domain.auth.service.AuthService;
import com.umatgong.global.kakao.KakaoAuthClient;
import com.umatgong.global.kakao.KakaoProperties;
import com.umatgong.global.security.JwtProperties;
import com.umatgong.global.security.JwtProvider;

/**
 * 배포 설정(application.yml + 환경변수)만으로 전체 컨텍스트가 뜨는지 본다. 프로필 없이 띄운다.
 */
@SpringBootTest
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class ApplicationContextIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void 인증에_필요한_빈이_모두_뜬다() {
		assertThat(context.getBean(SecurityFilterChain.class)).isNotNull();
		// Spring MVC도 CorsConfigurationSource를 하나 등록하므로 우리 빈은 이름으로 찾는다.
		assertThat(context.getBean("corsConfigurationSource", CorsConfigurationSource.class)).isNotNull();
		assertThat(context.getBean(JwtProvider.class)).isNotNull();
		assertThat(context.getBean(KakaoAuthClient.class)).isNotNull();
		assertThat(context.getBean(AuthService.class)).isNotNull();
		assertThat(context.getBean(AuthController.class)).isNotNull();
	}

	@Test
	void 환경변수가_설정에_바인딩된다() {
		JwtProperties jwt = context.getBean(JwtProperties.class);
		assertThat(jwt.accessTokenValidity().toMinutes()).isEqualTo(30);
		assertThat(jwt.refreshTokenValidity().toDays()).isEqualTo(14);
		assertThat(context.getBean(KakaoProperties.class).appId()).isNotNull();
	}
}

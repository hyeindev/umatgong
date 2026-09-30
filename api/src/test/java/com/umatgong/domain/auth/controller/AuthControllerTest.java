package com.umatgong.domain.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umatgong.domain.auth.dto.LoginResponse;
import com.umatgong.domain.auth.dto.TokenResponse;
import com.umatgong.domain.auth.service.AuthService;
import com.umatgong.global.config.CorsConfig;
import com.umatgong.global.config.CorsProperties;
import com.umatgong.global.config.SecurityConfig;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.security.JwtProvider;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class})
// 슬라이스 테스트에서는 애플리케이션의 @ConfigurationPropertiesScan이 적용되지 않는다.
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = "umatgong.cors.allowed-origins=https://web.umatgong.test")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtProvider jwtProvider;

	@Test
	void 카카오_로그인은_인증_없이_호출되고_토큰과_사용자를_돌려준다() throws Exception {
		given(authService.loginWithKakao("kakao-token")).willReturn(new LoginResponse(
			new TokenResponse("access", 1800, "refresh", 1_209_600),
			new LoginResponse.UserSummary(7L, "지현", null), true));

		mockMvc.perform(post("/api/auth/kakao")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"kakaoAccessToken\":\"kakao-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accessToken").value("access"))
			.andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
			.andExpect(jsonPath("$.data.refreshToken").value("refresh"))
			.andExpect(jsonPath("$.data.user.id").value(7))
			.andExpect(jsonPath("$.data.newUser").value(true))
			// 카카오 토큰이나 키가 응답에 섞여 나가지 않는다.
			.andExpect(jsonPath("$.data.kakaoAccessToken").doesNotExist());
	}

	@Test
	void 토큰이_비어_있으면_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(post("/api/auth/kakao")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"kakaoAccessToken\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 갱신에_실패하면_INVALID_REFRESH_TOKEN으로_401이다() throws Exception {
		given(authService.refresh("bad")).willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"bad\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void 로그아웃은_액세스_토큰_없이도_된다() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"mine\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
		verify(authService).logout("mine");
	}

	@Test
	void 웹_프론트_도메인의_CORS_사전요청을_허용한다() throws Exception {
		mockMvc.perform(options("/api/auth/kakao")
				.header("Origin", "https://web.umatgong.test")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "https://web.umatgong.test"));
	}

	@Test
	void 등록되지_않은_도메인의_CORS_요청은_거부한다() throws Exception {
		mockMvc.perform(options("/api/auth/kakao")
				.header("Origin", "https://evil.example")
				.header("Access-Control-Request-Method", "POST"))
			.andExpect(status().isForbidden());
	}
}

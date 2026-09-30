package com.umatgong.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.global.config.CorsConfig;
import com.umatgong.global.config.CorsProperties;
import com.umatgong.global.config.SecurityConfig;
import com.umatgong.global.response.ApiResponse;

/**
 * 실제 SecurityFilterChain + 실제 JwtProvider로 필터가 만드는 응답을 확인한다.
 */
@WebMvcTest(controllers = JwtAuthenticationFilterTest.ProbeController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtProvider.class, JwtAuthenticationFilterTest.ProbeController.class})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@TestPropertySource(properties = {
	"umatgong.jwt.secret=" + JwtAuthenticationFilterTest.SECRET,
	"umatgong.jwt.access-token-validity=1800",
	"umatgong.jwt.refresh-token-validity=1209600",
	"umatgong.cors.allowed-origins=http://localhost:8081"
})
class JwtAuthenticationFilterTest {

	static final String SECRET = "filter-test-secret-filter-test-secret-0123456789";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 토큰이_없으면_401_UNAUTHORIZED() throws Exception {
		mockMvc.perform(get("/api/probe/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 유효한_토큰이면_통과하고_사용자_ID가_주입된다() throws Exception {
		mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, bearer(jwtProvider.createAccessToken(7L))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(7));
	}

	@Test
	void 만료된_토큰이면_401_EXPIRED_TOKEN() throws Exception {
		String expired = new JwtProvider(new JwtProperties(SECRET, Duration.ofSeconds(-60), Duration.ofDays(14)))
			.createAccessToken(7L);

		mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("EXPIRED_TOKEN"));
	}

	@Test
	void 변조된_토큰이면_401_INVALID_TOKEN() throws Exception {
		String token = jwtProvider.createAccessToken(7L);
		String tampered = token.substring(0, token.length() - 2) + "xx";

		mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	void Bearer_형식이_아니면_토큰이_없는_것으로_본다() throws Exception {
		mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, jwtProvider.createAccessToken(7L)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void 인증이_필요_없는_경로는_만료된_토큰이_붙어_있어도_통과한다() throws Exception {
		String expired = new JwtProvider(new JwtProperties(SECRET, Duration.ofSeconds(-60), Duration.ofDays(14)))
			.createAccessToken(7L);

		mockMvc.perform(get("/api/auth/probe").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
			.andExpect(status().isOk());
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	@RestController
	static class ProbeController {

		@GetMapping("/api/probe/me")
		ApiResponse<Long> me(@AuthenticationPrincipal CustomUserDetails user) {
			return ApiResponse.ok(user.getUserId());
		}

		@GetMapping("/api/auth/probe")
		ApiResponse<Void> publicProbe() {
			return ApiResponse.ok();
		}
	}
}

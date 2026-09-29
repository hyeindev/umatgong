package com.umatgong.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.JwtAuthenticationFilter;
import com.umatgong.global.security.JwtProvider;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtProvider jwtProvider;
	private final ObjectMapper objectMapper;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
		throws Exception {
		http
			// 토큰을 Authorization 헤더로 받으므로 CSRF 대상이 아니다. 쿠키 방식으로 바꾸면 다시 켜야 한다.
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// /error를 막으면 실제 오류가 전부 401로 가려진다.
				.requestMatchers("/error").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, e) -> {
					ErrorCode code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) instanceof ErrorCode c
						? c : ErrorCode.UNAUTHORIZED;
					writeError(response, code);
				})
				.accessDeniedHandler((request, response, e) -> writeError(response, ErrorCode.FORBIDDEN)))
			// 빈으로 등록하면 서블릿 필터로도 자동 등록돼 보안 체인 밖에서 한 번 더 돈다. 그래서 직접 생성한다.
			.addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	// 필터 단계 오류는 GlobalExceptionHandler에 닿지 않으므로 같은 응답 형식을 여기서 직접 쓴다.
	private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
		response.setStatus(code.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code));
	}
}

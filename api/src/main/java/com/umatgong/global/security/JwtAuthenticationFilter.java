package com.umatgong.global.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.umatgong.global.error.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 토큰이 잘못돼도 여기서 요청을 끊지 않는다. 인증이 필요 없는 경로는 오래된 토큰이 붙어 있어도
 * 통과해야 하기 때문이다. 대신 실패 사유를 요청 속성에 남겨 두고, 인증이 필요한 경로에서
 * 진입점이 그 사유(EXPIRED_TOKEN 등)로 응답한다. 프론트는 그 코드를 보고 갱신 여부를 정한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_ERROR";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null) {
			try {
				CustomUserDetails principal = new CustomUserDetails(jwtProvider.parseAccessToken(token));
				SecurityContextHolder.getContext().setAuthentication(
					UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
			} catch (ExpiredJwtException e) {
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.EXPIRED_TOKEN);
			} catch (JwtException | IllegalArgumentException e) {
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.INVALID_TOKEN);
			}
		}
		chain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length());
	}
}

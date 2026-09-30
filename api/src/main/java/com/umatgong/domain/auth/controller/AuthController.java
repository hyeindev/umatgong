package com.umatgong.domain.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.auth.dto.KakaoCodeLoginRequest;
import com.umatgong.domain.auth.dto.KakaoLoginRequest;
import com.umatgong.domain.auth.dto.LoginResponse;
import com.umatgong.domain.auth.dto.RefreshTokenRequest;
import com.umatgong.domain.auth.dto.TokenResponse;
import com.umatgong.domain.auth.service.AuthService;
import com.umatgong.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// 네이티브: 카카오 네이티브 SDK가 준 액세스 토큰
	@PostMapping("/kakao")
	public ApiResponse<LoginResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
		return ApiResponse.ok(authService.loginWithKakao(request.kakaoAccessToken()));
	}

	// 웹: 카카오 JS SDK authorize가 준 인가 코드. 토큰 교환에 REST API 키가 필요해 서버가 대신 한다.
	@PostMapping("/kakao/code")
	public ApiResponse<LoginResponse> loginWithKakaoCode(@Valid @RequestBody KakaoCodeLoginRequest request) {
		return ApiResponse.ok(authService.loginWithKakaoCode(request.code(), request.redirectUri()));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.ok(authService.refresh(request.refreshToken()));
	}

	// 액세스 토큰이 만료된 상태에서도 로그아웃할 수 있어야 하므로 리프레시 토큰만으로 처리한다.
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request.refreshToken());
		return ApiResponse.ok();
	}
}

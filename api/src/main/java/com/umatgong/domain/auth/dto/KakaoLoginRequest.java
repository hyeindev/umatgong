package com.umatgong.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** @param kakaoAccessToken 클라이언트가 카카오 SDK 로그인으로 받은 액세스 토큰 */
public record KakaoLoginRequest(@NotBlank String kakaoAccessToken) {
}

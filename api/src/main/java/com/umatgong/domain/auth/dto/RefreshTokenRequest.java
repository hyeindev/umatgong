package com.umatgong.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** /api/auth/refresh와 /api/auth/logout이 같이 쓴다. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}

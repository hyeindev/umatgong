package com.umatgong.domain.auth.dto;

/**
 * @param accessTokenExpiresIn  초 단위
 * @param refreshTokenExpiresIn 초 단위. 회전할 때마다 다시 14일로 늘어난다.
 */
public record TokenResponse(
	String accessToken,
	long accessTokenExpiresIn,
	String refreshToken,
	long refreshTokenExpiresIn
) {
}

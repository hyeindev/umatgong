package com.umatgong.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 웹 로그인. 카카오 JS SDK authorize가 리다이렉트로 준 인가 코드.
 *
 * @param code        인가 코드. 한 번만 쓸 수 있다
 * @param redirectUri authorize에 보낸 값과 문자열까지 같아야 한다. 서버 허용 목록에 있어야 한다
 */
public record KakaoCodeLoginRequest(
	@NotBlank @Size(max = 1024) String code,
	@NotBlank @Size(max = 2048) String redirectUri) {
}

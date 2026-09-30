package com.umatgong.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프론트는 enum 이름(code)으로 분기한다. 이름을 바꾸면 클라이언트가 깨지므로
 * 한번 배포된 코드는 이름을 유지하고, 필요하면 새 코드를 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	// 프론트는 이 코드를 받으면 리프레시 토큰으로 갱신 후 재시도한다.
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

	// 인증
	// 카카오 액세스 토큰이 만료·위조됐거나 우리 앱에서 발급한 토큰이 아니다. 카카오 SDK로 다시 로그인한다.
	KAKAO_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
	KAKAO_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "카카오 서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요."),
	// 리프레시 토큰이 없거나 만료·폐기됐다. 프론트는 갱신을 멈추고 로그인 화면으로 보낸다.
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요.");

	private final HttpStatus status;
	private final String message;
}

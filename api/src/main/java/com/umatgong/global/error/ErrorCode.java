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
	// 웹 로그인의 인가 코드가 만료·재사용됐거나 redirectUri가 authorize 때와 다를 때도 이 코드다.
	KAKAO_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
	// 웹 로그인의 redirectUri가 서버 허용 목록에 없다. 오픈 리다이렉트를 막기 위해 카카오를 부르지 않고 거부한다.
	KAKAO_REDIRECT_URI_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 로그인 리다이렉트 주소입니다."),
	KAKAO_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "카카오 서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요."),
	// 리프레시 토큰이 없거나 만료·폐기됐다. 프론트는 갱신을 멈추고 로그인 화면으로 보낸다.
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),

	// 클럽
	// 없는 클럽과 내가 속하지 않은 클럽을 구분하지 않는다. 다른 클럽이 있다는 사실도 드러내지 않기 위해서다.
	CLUB_NOT_FOUND(HttpStatus.NOT_FOUND, "클럽을 찾을 수 없습니다."),
	INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "초대 링크가 올바르지 않거나 만료되었습니다."),
	ALREADY_CLUB_MEMBER(HttpStatus.CONFLICT, "이미 이 클럽의 멤버입니다."),
	// 정원이 다 찼다. 새 초대·합류만 막고 기존 멤버는 그대로 쓴다.
	CLUB_FULL(HttpStatus.CONFLICT, "클럽 정원이 다 찼습니다."),
	// 한 사람이 속할 수 있는 클럽 수를 넘었다.
	CLUB_LIMIT_REACHED(HttpStatus.CONFLICT, "더 이상 클럽에 들어갈 수 없습니다."),

	// 장소·기록
	// 없는 장소와 다른 클럽의 커스텀 장소를 구분하지 않는다.
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
	// 없는 기록과 내게 보이지 않는 기록(다른 클럽, 남의 비공개)을 구분하지 않는다.
	VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."),

	// 사진
	// 그림이 아니거나, 너무 크거나(원본), 크기 제한을 넘었다. 프론트가 썸네일로 줄여 다시 보낸다.
	PHOTO_INVALID(HttpStatus.BAD_REQUEST, "올릴 수 없는 사진입니다."),
	// 사진 저장소가 설정되지 않았거나 응답하지 않는다. 사진 없이 기록하도록 안내한다.
	PHOTO_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "지금은 사진을 올릴 수 없습니다.");

	private final HttpStatus status;
	private final String message;
}

// docs/api-spec.md 1장 “공통” 기준.

/** 서버가 내려주는 에러 코드. 분기는 message가 아니라 이 값으로 한다 */
export type ServerErrorCode =
  | 'INVALID_INPUT'
  | 'UNAUTHORIZED'
  | 'INVALID_TOKEN'
  | 'EXPIRED_TOKEN'
  | 'KAKAO_INVALID_TOKEN'
  | 'INVALID_REFRESH_TOKEN'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'METHOD_NOT_ALLOWED'
  | 'KAKAO_UNAVAILABLE'
  | 'INTERNAL_ERROR';

/**
 * 클라이언트에서만 생기는 에러 코드.
 * NETWORK_ERROR: 서버에 닿지 못함 / INVALID_RESPONSE: ApiResponse 형식이 아닌 응답
 */
export type ClientErrorCode = 'NETWORK_ERROR' | 'INVALID_RESPONSE';

export type ErrorCode = ServerErrorCode | ClientErrorCode;

export type FieldError = {
  field: string;
  reason: string;
};

export type ApiErrorBody = {
  code: ServerErrorCode;
  message: string;
  fieldErrors?: FieldError[];
};

export type ApiResponse<T> = { success: true; data?: T } | { success: false; error: ApiErrorBody };

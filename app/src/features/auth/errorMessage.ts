import { isApiError } from '@/lib/api';
import { KakaoLoginError } from '@/lib/kakao';

/** 로그인 실패를 사용자에게 보여줄 문구로 바꾼다 */
export const loginErrorMessage = (error: unknown) => {
  if (error instanceof KakaoLoginError) {
    return error.message;
  }
  if (isApiError(error, 'KAKAO_INVALID_TOKEN')) {
    return '카카오 로그인이 만료됐어요. 다시 시도해 주세요.';
  }
  if (isApiError(error, 'KAKAO_UNAVAILABLE')) {
    return '카카오 서버가 응답하지 않아요. 잠시 후 다시 시도해 주세요.';
  }
  if (isApiError(error)) {
    return error.message;
  }
  return '로그인에 실패했어요. 다시 시도해 주세요.';
};

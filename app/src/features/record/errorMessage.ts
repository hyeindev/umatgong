import { isApiError } from '@/lib/api';

export const recordErrorMessage = (error: unknown) => {
  if (isApiError(error, 'KAKAO_UNAVAILABLE')) {
    return '가게 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.';
  }
  if (isApiError(error, 'PHOTO_STORAGE_UNAVAILABLE')) {
    return '지금은 사진을 올릴 수 없어요. 사진 없이 기록할 수 있어요.';
  }
  if (isApiError(error, 'PHOTO_INVALID')) {
    return '올릴 수 없는 사진이에요. 다른 사진을 골라 주세요.';
  }
  if (isApiError(error, 'CLUB_NOT_FOUND')) {
    return '그 클럽에 기록할 수 없어요. 클럽 목록을 확인해 주세요.';
  }
  if (isApiError(error, 'PLACE_NOT_FOUND')) {
    return '그 가게를 찾을 수 없어요. 다시 골라 주세요.';
  }
  if (isApiError(error, 'NETWORK_ERROR')) {
    return '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.';
  }
  if (error instanceof Error && !isApiError(error)) {
    return error.message;
  }
  return '저장하지 못했어요. 잠시 후 다시 시도해 주세요.';
};

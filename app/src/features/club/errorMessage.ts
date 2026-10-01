import { isApiError } from '@/lib/api';

/** 클럽 API 실패를 보여줄 문구로 바꾼다. 정원·클럽 수 숫자는 서버 설정값이라 문구에 넣지 않는다 */
export const clubErrorMessage = (error: unknown) => {
  if (isApiError(error, 'CLUB_FULL')) {
    return '정원이 다 찼어요. 지금 멤버는 그대로 쓸 수 있고, 새로 들어오는 것만 막혀요.';
  }
  if (isApiError(error, 'CLUB_LIMIT_REACHED')) {
    return '지금은 더 이상 클럽에 들어갈 수 없어요. 다른 클럽에서 나오면 들어갈 수 있어요.';
  }
  if (isApiError(error, 'INVALID_INVITE_CODE')) {
    return '초대 링크가 올바르지 않거나 만료됐어요. 친구에게 새 링크를 받아 주세요.';
  }
  if (isApiError(error, 'ALREADY_CLUB_MEMBER')) {
    return '이미 이 클럽의 멤버예요.';
  }
  if (isApiError(error, 'CLUB_NOT_FOUND')) {
    return '클럽을 찾을 수 없어요.';
  }
  if (isApiError(error, 'INVALID_INPUT')) {
    return '클럽 이름을 1~30자로 적어 주세요.';
  }
  if (isApiError(error, 'NETWORK_ERROR')) {
    return '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.';
  }
  return '잠시 후 다시 시도해 주세요.';
};

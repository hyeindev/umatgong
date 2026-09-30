import { tokenStorage, type StoredSession } from '@/lib/storage';
import type { TokenResponse } from '@/types/auth';

import { ApiError, isApiError } from './errors';
import { send } from './http';

// 액세스 토큰은 메모리에만 둔다 (API 명세). 앱을 다시 열면 리프레시 토큰으로 새로 받는다.
let accessToken: string | null = null;

export const getAccessToken = () => accessToken;

type SessionEndedListener = () => void;
const sessionEndedListeners = new Set<SessionEndedListener>();

/** 로그인이 끝났을 때(갱신 실패, 로그아웃) 알림을 받는다. 반환값을 부르면 구독을 끊는다 */
export const onSessionEnded = (listener: SessionEndedListener) => {
  sessionEndedListeners.add(listener);
  return () => {
    sessionEndedListeners.delete(listener);
  };
};

/** 로그인 응답을 받은 직후에 부른다 */
export const startSession = async (tokens: TokenResponse, user: StoredSession['user']) => {
  await tokenStorage.save({ refreshToken: tokens.refreshToken, user });
  accessToken = tokens.accessToken;
};

/** 기기에 남은 로그인 정보를 지우고 로그인 화면으로 보낸다 */
export const endSession = async () => {
  accessToken = null;
  await tokenStorage.clear();
  sessionEndedListeners.forEach((listener) => listener());
};

let refreshing: Promise<string> | null = null;

/**
 * 리프레시 토큰으로 새 액세스 토큰을 받는다.
 * 여러 요청이 동시에 부르면 갱신은 하나만 보내고 나머지는 그 결과를 기다린다.
 * 리프레시 토큰이 무효(INVALID_REFRESH_TOKEN)면 로그인을 끝낸다.
 * 네트워크 오류면 로그인 정보를 그대로 두고 에러만 던진다 (다음 요청에서 다시 시도).
 */
export const refreshAccessToken = () => {
  refreshing ??= tokenStorage
    .exclusive(async () => {
      // 다른 탭이 먼저 갱신했을 수 있으므로 잠금 안에서 저장된 값을 다시 읽는다
      const stored = await tokenStorage.load();
      if (!stored) {
        throw new ApiError(401, 'INVALID_REFRESH_TOKEN', '다시 로그인해 주세요.');
      }
      const tokens = await send<TokenResponse>('POST', '/api/auth/refresh', {
        body: { refreshToken: stored.refreshToken },
      });
      // 보낸 리프레시 토큰은 폐기됐으므로 반드시 새 값으로 바꿔 둔다
      await tokenStorage.save({ ...stored, refreshToken: tokens.refreshToken });
      accessToken = tokens.accessToken;
      return tokens.accessToken;
    })
    .catch(async (error: unknown) => {
      if (isApiError(error, 'INVALID_REFRESH_TOKEN')) {
        await endSession();
      }
      throw error;
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
};

import {
  apiClient,
  endSession,
  isApiError,
  onSessionEnded,
  queryClient,
  refreshAccessToken,
  startSession,
} from '@/lib/api';
import { kakaoLogin, type KakaoCredential, type KakaoRedirectParams } from '@/lib/kakao';
import { tokenStorage } from '@/lib/storage';
import { useAuthStore } from '@/stores/auth';
import type { KakaoCodeLoginRequest, KakaoTokenLoginRequest, LoginResponse } from '@/types/auth';

// 갱신 실패 등으로 로그인이 끝나면 화면 상태도 로그아웃으로 바꾼다 (라우터가 로그인 화면으로 보낸다)
onSessionEnded(() => {
  queryClient.clear();
  useAuthStore.getState().setSignedOut();
});

/** 카카오가 준 값을 우리 JWT로 바꾼다. 웹은 인가 코드, 네이티브는 액세스 토큰 */
const exchangeCredential = (credential: KakaoCredential) => {
  if (credential.kind === 'code') {
    const body: KakaoCodeLoginRequest = {
      code: credential.code,
      redirectUri: credential.redirectUri,
    };
    return apiClient.post<LoginResponse>('/api/auth/kakao/code', body, { auth: false });
  }
  const body: KakaoTokenLoginRequest = { kakaoAccessToken: credential.accessToken };
  return apiClient.post<LoginResponse>('/api/auth/kakao', body, { auth: false });
};

const signInWithCredential = async (credential: KakaoCredential) => {
  const response = await exchangeCredential(credential);
  await startSession(response, response.user);
  useAuthStore.getState().setSignedIn(response.user);
  return response;
};

/**
 * 카카오 로그인 버튼. 웹은 카카오 페이지로 이동하고, 결과는 completeKakaoRedirect가 받는다.
 * 결과를 바로 받는 플랫폼은 여기서 로그인까지 끝낸다.
 */
export const signInWithKakao = async () => {
  const credential = await kakaoLogin.start();
  if (credential) {
    await signInWithCredential(credential);
  }
};

/** 카카오 로그인 콜백 화면에서 부른다 */
export const completeKakaoRedirect = async (params: KakaoRedirectParams) =>
  signInWithCredential(kakaoLogin.completeRedirect(params));

/**
 * 앱 시작 시 한 번 부른다. 저장된 리프레시 토큰으로 새 액세스 토큰을 받아 로그인이 살아 있는지 확인한다.
 */
export const restoreSession = async () => {
  const { setSignedIn, setSignedOut } = useAuthStore.getState();
  const stored = await tokenStorage.load();
  if (!stored) {
    setSignedOut();
    return;
  }
  try {
    await refreshAccessToken();
  } catch (error) {
    // INVALID_REFRESH_TOKEN이면 refreshAccessToken이 이미 로그인을 끝냈다
    if (isApiError(error, 'INVALID_REFRESH_TOKEN')) {
      return;
    }
    // 오프라인·서버 오류는 로그인이 끊긴 게 아니다. 저장된 로그인을 믿고 다음 요청에서 다시 갱신한다
  }
  setSignedIn(stored.user);
};

/** 이 기기의 로그인만 끝낸다. 서버 호출이 실패해도 기기의 토큰은 지운다 */
export const signOut = async () => {
  const stored = await tokenStorage.load();
  if (stored) {
    try {
      await apiClient.post(
        '/api/auth/logout',
        { refreshToken: stored.refreshToken },
        { auth: false },
      );
    } catch {
      // 서버가 폐기하지 못한 리프레시 토큰은 14일 뒤 만료된다
    }
  }
  await endSession();
};

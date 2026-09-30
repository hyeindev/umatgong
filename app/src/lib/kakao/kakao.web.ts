import { KakaoLoginError, type KakaoLogin } from './kakao.types';
import { getKakaoRedirectUri } from './redirect';
import { loadKakaoSdk } from './sdk.web';

// authorize에 보낸 state를 콜백에서 확인한다. 다른 곳에서 만든 로그인 요청이 끼어드는 것(CSRF)을 막는다.
const STATE_KEY = 'umatgong.kakao.state';

const createState = () => {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
};

export const kakaoLogin: KakaoLogin = {
  start: async () => {
    const kakao = await loadKakaoSdk();
    const state = createState();
    window.sessionStorage.setItem(STATE_KEY, state);
    kakao.Auth.authorize({ redirectUri: getKakaoRedirectUri(), state });
    // 카카오 페이지로 이동한다. 결과는 콜백 화면이 받는다
    return null;
  },

  completeRedirect: ({ code, state, error, errorDescription }) => {
    const expectedState = window.sessionStorage.getItem(STATE_KEY);
    window.sessionStorage.removeItem(STATE_KEY);

    if (error) {
      if (error === 'access_denied') {
        throw new KakaoLoginError('cancelled', '카카오 로그인을 취소했습니다.');
      }
      throw new KakaoLoginError('failed', errorDescription ?? '카카오 로그인에 실패했습니다.');
    }
    if (!code) {
      throw new KakaoLoginError('failed', '카카오 인가 코드가 없습니다.');
    }
    if (!expectedState || state !== expectedState) {
      throw new KakaoLoginError('failed', '로그인 요청을 확인할 수 없습니다. 다시 시도해 주세요.');
    }
    return { kind: 'code', code, redirectUri: getKakaoRedirectUri() };
  },
};

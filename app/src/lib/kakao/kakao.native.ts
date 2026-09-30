import { KakaoLoginError, type KakaoLogin } from './kakao.types';

// TODO: 카카오 네이티브 SDK로 로그인하고 { kind: 'accessToken' }을 돌려준다.
// 네이티브는 리다이렉트를 쓰지 않으므로 completeRedirect는 쓰이지 않는다.
export const kakaoLogin: KakaoLogin = {
  start: async () => {
    throw new KakaoLoginError('unsupported', '앱의 카카오 로그인은 아직 준비 중입니다.');
  },
  completeRedirect: () => {
    throw new KakaoLoginError('unsupported', '앱에서는 이 경로를 쓰지 않습니다.');
  },
};

// 번들러가 플랫폼에 맞춰 kakao.web.ts / kakao.native.ts 중 하나를 고른다.
export { kakaoLogin } from './kakao';
export { KAKAO_REDIRECT_PATH } from './redirect';
export {
  KakaoLoginError,
  type KakaoCredential,
  type KakaoLogin,
  type KakaoRedirectParams,
} from './kakao.types';

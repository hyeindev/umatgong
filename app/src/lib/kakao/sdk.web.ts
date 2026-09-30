import { KakaoLoginError } from './kakao.types';

// 카카오 JS SDK. 버전을 올리면 무결성 값도 카카오 개발자 사이트 [JavaScript SDK 다운로드]의 값으로 함께 바꾼다.
const SDK_VERSION = '2.8.1';
const SDK_INTEGRITY = 'sha384-OL+ylM/iuPLtW5U3XcvLSGhE8JzReKDank5InqlHGWPhb4140/yrBw0bg0y7+C9J';
const SDK_URL = `https://t1.kakaocdn.net/kakao_js_sdk/${SDK_VERSION}/kakao.min.js`;

// 브라우저에 노출되는 키다. 카카오 콘솔에서 JavaScript SDK 도메인을 반드시 제한한다.
const JS_KEY = process.env.EXPO_PUBLIC_KAKAO_JS_KEY;

/** 쓰는 기능만 적은 최소 타입 */
export type KakaoSdk = {
  init: (key: string) => void;
  isInitialized: () => boolean;
  Auth: {
    authorize: (options: { redirectUri: string; state?: string; prompt?: string }) => void;
  };
};

declare global {
  interface Window {
    Kakao?: KakaoSdk;
  }
}

let loading: Promise<KakaoSdk> | null = null;

const injectScript = () =>
  new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = SDK_URL;
    script.integrity = SDK_INTEGRITY;
    script.crossOrigin = 'anonymous';
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Kakao SDK load failed'));
    document.head.appendChild(script);
  });

/** SDK를 한 번만 불러오고 초기화한다 */
export const loadKakaoSdk = () => {
  loading ??= (async () => {
    if (!JS_KEY) {
      throw new KakaoLoginError('failed', 'EXPO_PUBLIC_KAKAO_JS_KEY가 설정되지 않았습니다.');
    }
    if (!window.Kakao) {
      await injectScript();
    }
    const kakao = window.Kakao;
    if (!kakao) {
      throw new KakaoLoginError('failed', '카카오 SDK를 불러오지 못했습니다.');
    }
    if (!kakao.isInitialized()) {
      kakao.init(JS_KEY);
    }
    return kakao;
  })().catch((error: unknown) => {
    // 실패하면 다음 시도에서 다시 불러온다
    loading = null;
    if (error instanceof KakaoLoginError) {
      throw error;
    }
    throw new KakaoLoginError(
      'failed',
      '카카오 SDK를 불러오지 못했습니다. 네트워크를 확인하거나 SDK 무결성 값을 확인해 주세요.',
    );
  });
  return loading;
};

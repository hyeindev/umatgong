/**
 * 카카오 로그인 콜백 경로. expo-router 라우트 src/app/auth/kakao/callback.tsx와 같아야 한다.
 * 카카오 콘솔 [카카오 로그인 > Redirect URI]에 “출처 + 이 경로”를 등록한다.
 */
export const KAKAO_REDIRECT_PATH = '/auth/kakao/callback';

/**
 * authorize 요청과 코드 교환 요청(POST /api/auth/kakao/code)에 보내는 redirectUri.
 * 두 값이 문자열까지 같아야 카카오가 코드를 바꿔 주므로, 반드시 이 함수 하나로만 만든다.
 */
export const getKakaoRedirectUri = () => `${window.location.origin}${KAKAO_REDIRECT_PATH}`;

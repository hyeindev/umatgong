// docs/api-spec.md 2장 “인증” 기준.

export type AuthUser = {
  id: number;
  /** 카카오 닉네임. 동의하지 않았으면 "이름 없음" */
  name: string;
  /** 카카오 프로필 이미지. 없거나 카카오 기본 이미지면 null */
  avatarUrl: string | null;
};

/** POST /api/auth/refresh 응답 */
export type TokenResponse = {
  accessToken: string;
  /** 초 */
  accessTokenExpiresIn: number;
  refreshToken: string;
  /** 초 */
  refreshTokenExpiresIn: number;
};

/** POST /api/auth/kakao, POST /api/auth/kakao/code 응답 */
export type LoginResponse = TokenResponse & {
  user: AuthUser;
  /** 이번에 가입했으면 true. 첫 실행 온보딩(S1)을 띄우는 기준 */
  newUser: boolean;
};

/** POST /api/auth/kakao — 네이티브: 카카오 SDK가 준 액세스 토큰 */
export type KakaoTokenLoginRequest = {
  kakaoAccessToken: string;
};

/**
 * POST /api/auth/kakao/code — 웹: 카카오 JS SDK authorize로 받은 인가 코드.
 * redirectUri는 authorize에 보낸 값과 문자열까지 같아야 한다.
 */
export type KakaoCodeLoginRequest = {
  code: string;
  redirectUri: string;
};

export type RefreshRequest = {
  refreshToken: string;
};

export type LogoutRequest = {
  refreshToken: string;
};

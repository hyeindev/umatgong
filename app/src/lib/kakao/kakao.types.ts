/**
 * 카카오 로그인 결과. 플랫폼마다 서버에 보내는 값이 다르다.
 * - 웹: JS SDK authorize가 준 인가 코드 → POST /api/auth/kakao/code
 * - 네이티브: 네이티브 SDK가 준 액세스 토큰 → POST /api/auth/kakao
 */
export type KakaoCredential =
  | { kind: 'code'; code: string; redirectUri: string }
  | { kind: 'accessToken'; accessToken: string };

/** 카카오 로그인 콜백으로 돌아온 주소의 쿼리 */
export type KakaoRedirectParams = {
  code?: string;
  state?: string;
  error?: string;
  errorDescription?: string;
};

export type KakaoLoginErrorReason =
  /** 사용자가 동의 화면에서 취소함 */
  | 'cancelled'
  /** 키 누락, SDK 로드 실패, state 불일치 등 */
  | 'failed'
  /** 이 플랫폼에서는 아직 구현 전 */
  | 'unsupported';

export class KakaoLoginError extends Error {
  readonly reason: KakaoLoginErrorReason;

  constructor(reason: KakaoLoginErrorReason, message: string) {
    super(message);
    this.name = 'KakaoLoginError';
    this.reason = reason;
  }
}

export type KakaoLogin = {
  /**
   * 카카오 로그인을 시작한다.
   * 결과를 바로 받는 플랫폼은 KakaoCredential을 돌려준다.
   * 웹은 카카오 페이지로 이동하므로 null을 돌려주고, 결과는 콜백 화면에서 completeRedirect로 받는다.
   */
  start: () => Promise<KakaoCredential | null>;
  /** 콜백 주소로 돌아왔을 때 쿼리를 검사해 KakaoCredential로 바꾼다 */
  completeRedirect: (params: KakaoRedirectParams) => KakaoCredential;
};

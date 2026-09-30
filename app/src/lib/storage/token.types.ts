export type Tokens = {
  accessToken: string;
  refreshToken: string;
};

/** 토큰 저장 방식만 플랫폼별로 다르다. API는 웹과 네이티브가 같다. */
export type TokenStorage = {
  get: () => Promise<Tokens | null>;
  set: (tokens: Tokens) => Promise<void>;
  clear: () => Promise<void>;
};

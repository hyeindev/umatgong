import type { TokenStorage } from './token.types';

// 아직 구현 전이다. 조용히 null을 돌려주면 로그인이 풀린 것처럼 보이므로 호출 즉시 실패시킨다.
const notImplemented = (): never => {
  throw new Error('tokenStorage is not implemented yet');
};

export const tokenStorage: TokenStorage = {
  get: async () => notImplemented(),
  set: async () => notImplemented(),
  clear: async () => notImplemented(),
};

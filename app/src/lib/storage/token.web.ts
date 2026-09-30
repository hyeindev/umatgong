import { createSerialQueue, parseStoredSession } from './parse';
import type { TokenStorage } from './token.types';

// 웹은 localStorage에 둔다. 새로고침·탭을 닫았다 열어도 로그인이 유지된다.
// 같은 출처의 스크립트는 읽을 수 있으므로 XSS에 약하다. 쿠키로 옮기려면 서버 CORS·CSRF도 함께 바꿔야 한다
// (docs/api-spec.md “인증”).
const KEY = 'umatgong.session';
const LOCK_NAME = 'umatgong.token-refresh';

const serial = createSerialQueue();

export const tokenStorage: TokenStorage = {
  load: async () => {
    try {
      return parseStoredSession(window.localStorage.getItem(KEY));
    } catch {
      // 사생활 보호 모드 등에서 저장소 접근이 막힌 경우
      return null;
    }
  },
  save: async (session) => {
    window.localStorage.setItem(KEY, JSON.stringify(session));
  },
  clear: async () => {
    try {
      window.localStorage.removeItem(KEY);
    } catch {
      // 지울 수 없으면 읽을 수도 없으므로 무시한다
    }
  },
  // 탭이 여러 개면 탭마다 갱신을 보낼 수 있다. Web Locks로 탭 사이에서도 하나씩만 돌린다.
  exclusive: (task) => {
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
    if (!locks) {
      return serial(task);
    }
    return locks.request(LOCK_NAME, task);
  },
};

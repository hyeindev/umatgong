import type { AuthUser } from '@/types/auth';

/**
 * 기기에 남겨 두는 로그인 정보.
 * 액세스 토큰은 여기에 두지 않는다 (API 명세: 메모리 보관). 앱을 다시 열면 리프레시 토큰으로 새로 받는다.
 * user는 서버에 내 정보 조회 API가 없어서 로그인 응답의 값을 함께 둔다.
 */
export type StoredSession = {
  refreshToken: string;
  user: AuthUser;
};

/** 저장 위치만 플랫폼별로 다르다. 화면과 API 클라이언트는 이 인터페이스만 쓴다 */
export type TokenStorage = {
  load: () => Promise<StoredSession | null>;
  save: (session: StoredSession) => Promise<void>;
  clear: () => Promise<void>;
  /**
   * 한 번에 하나만 돌아야 하는 작업(토큰 갱신)을 순서대로 실행한다.
   * 리프레시 토큰은 한 번 쓰면 폐기되므로, 같은 토큰으로 두 번 갱신하면 로그인이 끊긴다.
   */
  exclusive: <T>(task: () => Promise<T>) => Promise<T>;
};

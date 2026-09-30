import type { StoredSession } from './token.types';

/** 저장된 값이 깨졌거나 예전 형식이면 없는 것으로 본다 */
export const parseStoredSession = (raw: string | null): StoredSession | null => {
  if (!raw) {
    return null;
  }
  try {
    const value: unknown = JSON.parse(raw);
    if (typeof value !== 'object' || value === null) {
      return null;
    }
    const { refreshToken, user } = value as Partial<StoredSession>;
    if (
      typeof refreshToken !== 'string' ||
      typeof user !== 'object' ||
      user === null ||
      typeof user.id !== 'number' ||
      typeof user.name !== 'string' ||
      (user.avatarUrl !== null && typeof user.avatarUrl !== 'string')
    ) {
      return null;
    }
    return { refreshToken, user };
  } catch {
    return null;
  }
};

/** 같은 실행 안에서 작업을 순서대로 이어 붙인다 */
export const createSerialQueue = () => {
  let tail: Promise<unknown> = Promise.resolve();
  return <T>(task: () => Promise<T>): Promise<T> => {
    const run = tail.then(task, task);
    tail = run.catch(() => undefined);
    return run;
  };
};

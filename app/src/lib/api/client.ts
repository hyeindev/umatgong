import { isApiError } from './errors';
import { send, type HttpMethod, type QueryValue } from './http';
import { endSession, getAccessToken, refreshAccessToken } from './session';

export type RequestOptions = {
  body?: unknown;
  query?: Record<string, QueryValue>;
  /** false면 액세스 토큰을 붙이지 않는다 (인증 API) */
  auth?: boolean;
};

/**
 * 인증 API 클라이언트.
 * - 요청: 액세스 토큰을 Authorization 헤더에 넣는다. 메모리에 없으면 먼저 갱신한다
 * - 응답: EXPIRED_TOKEN·INVALID_TOKEN(401)이면 한 번 갱신하고 원 요청을 다시 보낸다.
 *   갱신이 실패하거나 다시 401이면 로그인을 끝낸다 (docs/api-spec.md “401 처리 규칙”)
 */
const request = async <T>(method: HttpMethod, path: string, options: RequestOptions = {}) => {
  const { auth = true, ...sendOptions } = options;
  if (!auth) {
    return send<T>(method, path, sendOptions);
  }

  const token = getAccessToken() ?? (await refreshAccessToken());
  try {
    return await send<T>(method, path, { ...sendOptions, accessToken: token });
  } catch (error) {
    if (isApiError(error, 'UNAUTHORIZED')) {
      await endSession();
      throw error;
    }
    if (!isApiError(error, 'EXPIRED_TOKEN', 'INVALID_TOKEN')) {
      throw error;
    }
  }

  const refreshed = await refreshAccessToken();
  try {
    return await send<T>(method, path, { ...sendOptions, accessToken: refreshed });
  } catch (error) {
    if (isApiError(error, 'UNAUTHORIZED', 'EXPIRED_TOKEN', 'INVALID_TOKEN')) {
      await endSession();
    }
    throw error;
  }
};

export const apiClient = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('GET', path, options),
  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('POST', path, { ...options, body }),
  put: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('PUT', path, { ...options, body }),
  patch: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('PATCH', path, { ...options, body }),
  delete: <T>(path: string, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('DELETE', path, options),
};

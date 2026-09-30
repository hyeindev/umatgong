import type { ApiResponse } from '@/types/api';

import { API_URL } from './config';
import { ApiError } from './errors';

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export type QueryValue = string | number | boolean | null | undefined;

export type SendOptions = {
  body?: unknown;
  query?: Record<string, QueryValue>;
  accessToken?: string | null;
};

const buildUrl = (path: string, query?: Record<string, QueryValue>) => {
  if (!API_URL) {
    throw new ApiError(0, 'NETWORK_ERROR', 'EXPO_PUBLIC_API_URL이 설정되지 않았습니다.');
  }
  const params = new URLSearchParams();
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      params.append(key, String(value));
    }
  });
  const search = params.toString();
  return `${API_URL}${path}${search ? `?${search}` : ''}`;
};

const isApiResponse = (value: unknown): value is ApiResponse<unknown> =>
  typeof value === 'object' &&
  value !== null &&
  typeof (value as { success?: unknown }).success === 'boolean';

/**
 * 요청 한 번을 보내고 ApiResponse를 벗긴다. 성공이면 data, 실패면 ApiError를 던진다.
 * 토큰 갱신·재시도는 여기서 하지 않는다 (client.ts).
 */
export const send = async <T>(method: HttpMethod, path: string, options: SendOptions = {}) => {
  const url = buildUrl(path, options.query);
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (options.accessToken) {
    headers.Authorization = `Bearer ${options.accessToken}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(
      0,
      'NETWORK_ERROR',
      '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.',
    );
  }

  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    payload = undefined;
  }

  if (!isApiResponse(payload)) {
    throw new ApiError(response.status, 'INVALID_RESPONSE', '서버 응답을 읽을 수 없습니다.');
  }
  if (!payload.success) {
    const { code, message, fieldErrors } = payload.error;
    throw new ApiError(response.status, code, message, fieldErrors);
  }
  return payload.data as T;
};

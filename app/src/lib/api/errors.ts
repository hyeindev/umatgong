import type { ErrorCode, FieldError } from '@/types/api';

/** API 호출 실패. 화면은 code로 분기한다 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: ErrorCode;
  readonly fieldErrors: FieldError[];

  constructor(status: number, code: ErrorCode, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export const isApiError = (error: unknown, ...codes: ErrorCode[]): error is ApiError =>
  error instanceof ApiError && (codes.length === 0 || codes.includes(error.code));

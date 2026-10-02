import type { Rating } from '@/types/visit';

/** 평가 3단계 라벨 (기획서·화면기획서 기준). 숫자 척도로 바꾸지 않는다 */
export const RATING_LABEL: Record<Rating, string> = {
  AGAIN: '또 갈래',
  OKAY: '괜찮아',
  NOPE: '한 번은',
};

export const RATING_HINT: Record<Rating, string> = {
  AGAIN: '친구에게 꼭 권할 곳',
  OKAY: '무난했던 곳',
  NOPE: '한 번이면 충분한 곳',
};

export const RATINGS: readonly Rating[] = ['AGAIN', 'OKAY', 'NOPE'];

// docs/api-spec.md 5장 “방문 기록” 기준.
import type { ClubColor } from '@/theme';

import type { Coordinate } from './geo';

/** 평가 3단계. 숫자 척도로 바꾸지 않는다 — AGAIN 또 갈래 / OKAY 괜찮아 / NOPE 한 번은 */
export type Rating = 'AGAIN' | 'OKAY' | 'NOPE';

/** GET /api/visits/map 한 건. 핀은 가볍게: 상세는 장소별 기록으로 받는다 */
export type VisitPin = {
  id: number;
  placeId: number;
  coordinate: Coordinate;
  /** 클럽 없이 남긴 기록이면 null */
  clubColor: ClubColor | null;
  rating: Rating;
  thumbnailUrl: string | null;
};

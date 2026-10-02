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

/** 기록 한 건 (GET /api/visits/nearby 등). 기록하기 화면은 후보 정렬에 placeId와 mine만 쓴다 */
export type Visit = {
  id: number;
  place: {
    id: number;
    name: string;
    address: string | null;
    category: string | null;
    coordinate: Coordinate;
  };
  club: { id: number; name: string; color: ClubColor } | null;
  author: { id: number; name: string; avatarUrl: string | null };
  rating: Rating;
  memo: string | null;
  visibility: 'CLUB' | 'PRIVATE';
  visitedAt: string;
  createdAt: string;
  thumbnailUrl: string | null;
  thumbnailUrls: string[];
  mine: boolean;
  distanceMeters: number | null;
};

/** POST /api/visits */
export type CreateVisitRequest = {
  placeId: number;
  clubId?: number;
  rating: Rating;
  memo?: string;
  visitedAt: string;
  thumbnailUrls?: string[];
};

/** POST /api/photos/thumbnails */
export type ThumbnailUpload = { url: string; width: number; height: number };

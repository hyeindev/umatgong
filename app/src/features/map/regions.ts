import type { MapBounds } from '@/lib/map';

/**
 * 남한 전체. 첫 화면은 여기서 시작한다 (docs/화면기획서.md 4.1, docs/design/ref-main.html).
 * 제주(남쪽 끝)와 울릉도·독도(동쪽 끝)까지 들어오게 잡는다.
 */
export const SOUTH_KOREA_BOUNDS: MapBounds = {
  sw: { lat: 33.1, lng: 125.0 },
  ne: { lat: 38.7, lng: 131.0 },
};

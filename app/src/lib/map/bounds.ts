// 지도 영역 제한 계산. SDK·React에 의존하지 않는 순수 함수만 둔다 (node --test로 돈다).
import type { Coordinate } from '@/types/geo';

import type { MapBounds } from './Map.types';

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

/** 지도 가운데가 영역 밖이면 영역 안의 가장 가까운 점으로. 안이면 null (옮길 필요 없음) */
export const clampCenter = (center: Coordinate, bounds: MapBounds): Coordinate | null => {
  const lat = clamp(center.lat, bounds.sw.lat, bounds.ne.lat);
  const lng = clamp(center.lng, bounds.sw.lng, bounds.ne.lng);
  return lat === center.lat && lng === center.lng ? null : { lat, lng };
};

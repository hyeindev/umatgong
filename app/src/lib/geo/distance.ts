// 거리 계산·표시. 순수 함수만 둔다 (node --test로 돈다).
import type { Coordinate } from '@/types/geo';

const EARTH_RADIUS_METERS = 6_371_008.8;
const toRad = (deg: number) => (deg * Math.PI) / 180;

/** 두 좌표 사이의 지표면 거리(미터, 하버사인). 표시용 직선거리다 */
export const distanceMeters = (a: Coordinate, b: Coordinate) => {
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(h)));
};

/** 120m / 1.4km / 23km */
export const formatDistance = (meters: number | null) => {
  if (meters === null || !Number.isFinite(meters)) {
    return null;
  }
  if (meters < 1000) {
    return `${Math.max(1, Math.round(meters / 10) * 10)}m`;
  }
  return meters < 10_000 ? `${(meters / 1000).toFixed(1)}km` : `${Math.round(meters / 1000)}km`;
};

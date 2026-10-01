import type { Coordinate } from '@/types/geo';

export type LocationResult =
  | { status: 'granted'; coordinate: Coordinate }
  /** 사용자가 위치 권한을 거부함 */
  | { status: 'denied' }
  /** 이 기기·브라우저에서 위치를 얻을 수 없음 (미지원, 시간 초과 등) */
  | { status: 'unavailable' };

export type LocationProvider = {
  getCurrent: () => Promise<LocationResult>;
};

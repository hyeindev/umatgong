import type { Coordinate } from '@/types/geo';

export type LocationResult =
  | { status: 'granted'; coordinate: Coordinate }
  /** 사용자가 위치 권한을 거부함 */
  | { status: 'denied' }
  /** 이 기기·브라우저에서 위치를 얻을 수 없음 (미지원, 시간 초과 등) */
  | { status: 'unavailable' };

export type LocationProvider = {
  /** 지금 위치. 권한이 없으면 묻는다 (사용자가 버튼을 눌렀을 때만 부른다) */
  getCurrent: () => Promise<LocationResult>;
  /** 이미 권한이 있을 때만 지금 위치. 권한을 묻지 않는다. 없거나 모르면 null (거리 표시 같은 곁다리용) */
  peek: () => Promise<Coordinate | null>;
};

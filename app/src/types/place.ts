// docs/api-spec.md 3장 “장소” 기준.
import type { Coordinate } from './geo';

export type Place = {
  /** 우리 서비스의 장소 ID. 기록을 남길 때 이 값을 쓴다 */
  id: number;
  name: string;
  /** 도로명 주소. 없으면 지번 주소 */
  address: string | null;
  /** 업종의 가장 구체적인 단계. 예: "곱창,막창" */
  category: string | null;
  coordinate: Coordinate;
  /** 요청 좌표로부터의 직선거리(미터). 요청에 좌표가 없으면 null */
  distanceMeters: number | null;
};

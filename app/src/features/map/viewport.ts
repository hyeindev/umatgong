// 지도 영역 계산. React·테마에 의존하지 않는 순수 함수만 둔다 (node --test로 바로 돈다).
import type { Coordinate } from '@/types/geo';

export type Bounds = { sw: Coordinate; ne: Coordinate };

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

// 아주 가깝게 확대해도 격자가 0이 되지 않게 한다 (약 1m)
const MIN_STEP = 2 ** -17;

// 2의 거듭제곱 격자. 영역 크기의 1/4 ~ 1/2 사이 칸이 된다.
const stepFor = (span: number) => 2 ** Math.floor(Math.log2(Math.max(span / 2, MIN_STEP)));

/**
 * 핀 조회 영역을 격자에 맞춰 바깥쪽으로 넓힌다.
 *
 * 보이는 영역을 그대로 쿼리 키로 쓰면 지도를 1px만 움직여도 새 요청이 된다. 격자에 맞추면
 * 조금 움직였거나 같은 곳으로 돌아왔을 때 키가 같아 TanStack Query 캐시를 그대로 쓴다.
 * 칸이 영역의 1/2 이하라 넓힌 영역은 축마다 보이는 영역의 2배를 넘지 않는다.
 */
export const snapBounds = ({ sw, ne }: Bounds): Bounds => {
  const latStep = stepFor(ne.lat - sw.lat);
  const lngStep = stepFor(ne.lng - sw.lng);
  return {
    sw: {
      lat: clamp(Math.floor(sw.lat / latStep) * latStep, -90, 90),
      lng: clamp(Math.floor(sw.lng / lngStep) * lngStep, -180, 180),
    },
    ne: {
      lat: clamp(Math.ceil(ne.lat / latStep) * latStep, -90, 90),
      lng: clamp(Math.ceil(ne.lng / lngStep) * lngStep, -180, 180),
    },
  };
};

/** 쿼리 키용 문자열. 격자에 맞춘 값이라 소수점 오차 없이 같은 영역은 같은 문자열이 된다 */
export const boundsKey = ({ sw, ne }: Bounds) => [sw.lat, sw.lng, ne.lat, ne.lng].join(',');

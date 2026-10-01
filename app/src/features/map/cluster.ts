// 화면 픽셀 격자로 가까운 핀을 묶는다. React·테마에 의존하지 않는 순수 함수만 둔다.
import type { Coordinate } from '@/types/geo';

import type { Bounds } from './viewport';

export type Viewport = {
  bounds: Bounds;
  /** 지도 화면의 픽셀 크기 */
  width: number;
  height: number;
};

export type Clustered<T> =
  | { kind: 'single'; item: T }
  | { kind: 'cluster'; key: string; coordinate: Coordinate; items: T[] };

/** 이 픽셀 안에 들어오는 핀은 하나로 묶는다. 핀이 손가락으로 따로 눌리는 최소 간격이다 */
export const CLUSTER_CELL_PX = 56;

const toRad = (deg: number) => (deg * Math.PI) / 180;
// 웹 메르카토르. 지도 SDK가 그리는 투영과 같아야 화면에서 가까운 핀이 실제로 묶인다
const mercatorY = (lat: number) => Math.log(Math.tan(Math.PI / 4 + toRad(lat) / 2));

/**
 * 격자는 화면이 아니라 세계 좌표에 고정한다. 같은 확대 단계에서 지도를 옮겨도 묶음이 바뀌지 않는다.
 * 확대 단계는 화면 너비 ÷ 보이는 경도 폭(픽셀/라디안)으로 알아낸다. 지도 SDK의 레벨 숫자를 몰라도 된다.
 */
export const clusterByGrid = <T extends { coordinate: Coordinate }>(
  items: readonly T[],
  viewport: Viewport,
  cellPx: number = CLUSTER_CELL_PX,
): Clustered<T>[] => {
  const { sw, ne } = viewport.bounds;
  const lngSpan = toRad(ne.lng - sw.lng);
  if (lngSpan <= 0 || viewport.width <= 0) {
    return items.map((item) => ({ kind: 'single', item }));
  }
  const pxPerRad = viewport.width / lngSpan;

  const cells = new Map<string, T[]>();
  for (const item of items) {
    const x = Math.floor((toRad(item.coordinate.lng) * pxPerRad) / cellPx);
    const y = Math.floor((-mercatorY(item.coordinate.lat) * pxPerRad) / cellPx);
    const key = `${x}:${y}`;
    const cell = cells.get(key);
    if (cell) {
      cell.push(item);
    } else {
      cells.set(key, [item]);
    }
  }

  return [...cells.entries()].map(([key, cell]): Clustered<T> => {
    if (cell.length === 1) {
      return { kind: 'single', item: cell[0] as T };
    }
    const lat = cell.reduce((sum, item) => sum + item.coordinate.lat, 0) / cell.length;
    const lng = cell.reduce((sum, item) => sum + item.coordinate.lng, 0) / cell.length;
    return { kind: 'cluster', key, coordinate: { lat, lng }, items: cell };
  });
};

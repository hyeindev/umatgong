/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test). Node 타입은 테스트 파일에만 들인다
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { clusterByGrid, type Viewport } from './cluster.ts';

const at = (id: number, lat: number, lng: number) => ({ id, coordinate: { lat, lng } });

// 너비 400px에 경도 0.04도 (동네 단계 정도). 1px ≈ 0.0001도
const NEIGHBORHOOD: Viewport = {
  bounds: { sw: { lat: 37.27, lng: 127.03 }, ne: { lat: 37.31, lng: 127.07 } },
  width: 400,
  height: 400,
};
// 같은 화면 크기에 경도 4도 (광역 단계)
const REGION: Viewport = {
  bounds: { sw: { lat: 35.5, lng: 125.5 }, ne: { lat: 39.5, lng: 129.5 } },
  width: 400,
  height: 400,
};

describe('clusterByGrid', () => {
  it('동네 단계에서 떨어진 핀은 따로 보인다', () => {
    const result = clusterByGrid([at(1, 37.28, 127.035), at(2, 37.3, 127.065)], NEIGHBORHOOD);
    assert.deepEqual(
      result.map((r) => r.kind),
      ['single', 'single'],
    );
  });

  it('멀리서 보면 같은 핀들이 하나로 묶이고 개수와 가운데 좌표를 가진다', () => {
    const items = [at(1, 37.28, 127.035), at(2, 37.3, 127.065)];
    const result = clusterByGrid(items, REGION);
    assert.equal(result.length, 1);
    const [cluster] = result;
    assert.ok(cluster && cluster.kind === 'cluster');
    assert.equal(cluster.items.length, 2);
    assert.ok(Math.abs(cluster.coordinate.lat - 37.29) < 1e-9);
    assert.ok(Math.abs(cluster.coordinate.lng - 127.05) < 1e-9);
  });

  it('화면 몇 픽셀 안의 핀은 동네 단계에서도 묶인다', () => {
    // 약 1px 차이
    const result = clusterByGrid(
      [at(1, 37.29001, 127.05001), at(2, 37.29002, 127.05002)],
      NEIGHBORHOOD,
    );
    assert.equal(result.length, 1);
    assert.equal(result[0]?.kind, 'cluster');
  });

  it('같은 확대 단계에서 지도를 옮겨도 묶음이 바뀌지 않는다', () => {
    const items = [at(1, 37.28, 127.035), at(2, 37.2801, 127.0351), at(3, 37.3, 127.065)];
    const moved: Viewport = {
      ...NEIGHBORHOOD,
      bounds: { sw: { lat: 37.275, lng: 127.0333 }, ne: { lat: 37.315, lng: 127.0733 } },
    };
    const shape = (v: Viewport) =>
      clusterByGrid(items, v)
        .map((r) => (r.kind === 'cluster' ? r.items.map((i) => i.id).join('+') : String(r.item.id)))
        .sort();
    assert.deepEqual(shape(moved), shape(NEIGHBORHOOD));
  });

  it('빈 목록이면 빈 결과', () => {
    assert.deepEqual(clusterByGrid([], NEIGHBORHOOD), []);
  });
});

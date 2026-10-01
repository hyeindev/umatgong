/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test). Node 타입은 테스트 파일에만 들인다
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { boundsKey, snapBounds, type Bounds } from './viewport.ts';

const contains = (outer: Bounds, inner: Bounds) =>
  outer.sw.lat <= inner.sw.lat &&
  outer.sw.lng <= inner.sw.lng &&
  outer.ne.lat >= inner.ne.lat &&
  outer.ne.lng >= inner.ne.lng;

// 광교 일대 (동네 단계 정도)
const GWANGGYO: Bounds = { sw: { lat: 37.27, lng: 127.03 }, ne: { lat: 37.31, lng: 127.08 } };

describe('snapBounds', () => {
  it('보이는 영역을 빠짐없이 덮는다', () => {
    assert.ok(contains(snapBounds(GWANGGYO), GWANGGYO));
  });

  it('조금 움직이면 같은 키 — 다시 요청하지 않는다', () => {
    const moved: Bounds = {
      sw: { lat: 37.2702, lng: 127.0305 },
      ne: { lat: 37.3102, lng: 127.0805 },
    };
    assert.equal(boundsKey(snapBounds(moved)), boundsKey(snapBounds(GWANGGYO)));
  });

  it('멀리 갔다가 돌아오면 처음과 같은 키', () => {
    const first = boundsKey(snapBounds(GWANGGYO));
    const away = boundsKey(
      snapBounds({ sw: { lat: 35.1, lng: 129.0 }, ne: { lat: 35.14, lng: 129.05 } }),
    );
    assert.notEqual(away, first);
    assert.equal(boundsKey(snapBounds(GWANGGYO)), first);
  });

  it('넓힌 영역은 축마다 보이는 영역의 2배를 넘지 않는다', () => {
    for (const span of [0.001, 0.01, 0.05, 0.3, 2, 6]) {
      for (const offset of [0, 0.0123, 0.4567]) {
        const b: Bounds = {
          sw: { lat: 36 + offset, lng: 127 + offset },
          ne: { lat: 36 + offset + span, lng: 127 + offset + span },
        };
        const s = snapBounds(b);
        assert.ok(contains(s, b));
        assert.ok(s.ne.lat - s.sw.lat <= span * 2 + 1e-9, `lat span ${span}`);
        assert.ok(s.ne.lng - s.sw.lng <= span * 2 + 1e-9, `lng span ${span}`);
      }
    }
  });

  it('위도·경도 범위를 넘지 않는다', () => {
    const s = snapBounds({ sw: { lat: -89.9, lng: -179.9 }, ne: { lat: 89.9, lng: 179.9 } });
    assert.ok(s.sw.lat >= -90 && s.ne.lat <= 90 && s.sw.lng >= -180 && s.ne.lng <= 180);
  });
});

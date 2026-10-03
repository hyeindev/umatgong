/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { distanceMeters, formatDistance } from './distance.ts';

describe('distanceMeters', () => {
  it('서울시청 ~ 부산시청은 약 325km', () => {
    const d = distanceMeters({ lat: 37.5663, lng: 126.9779 }, { lat: 35.1798, lng: 129.075 });
    assert.ok(d > 320_000 && d < 330_000, String(d));
  });

  it('같은 점은 0', () => {
    assert.equal(distanceMeters({ lat: 37.29, lng: 127.05 }, { lat: 37.29, lng: 127.05 }), 0);
  });
});

describe('formatDistance', () => {
  it('1km 미만은 10m 단위, 10km 미만은 소수 한 자리, 그 이상은 정수 km', () => {
    assert.equal(formatDistance(123), '120m');
    assert.equal(formatDistance(3), '1m');
    assert.equal(formatDistance(1449), '1.4km');
    assert.equal(formatDistance(23_400), '23km');
    assert.equal(formatDistance(null), null);
  });
});

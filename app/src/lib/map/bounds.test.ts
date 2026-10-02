/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { clampCenter } from './bounds.ts';

const KOREA = { sw: { lat: 33.1, lng: 125.0 }, ne: { lat: 38.7, lng: 131.0 } };

describe('clampCenter', () => {
  it('남한 안이면 그대로 둔다', () => {
    assert.equal(clampCenter({ lat: 37.29, lng: 127.05 }, KOREA), null);
  });

  it('밖으로 끌고 가면 가장 가까운 경계로 돌려놓는다', () => {
    // 베이징 쪽
    assert.deepEqual(clampCenter({ lat: 39.9, lng: 116.4 }, KOREA), { lat: 38.7, lng: 125.0 });
    // 도쿄 쪽
    assert.deepEqual(clampCenter({ lat: 35.68, lng: 139.7 }, KOREA), { lat: 35.68, lng: 131.0 });
    // 제주 남쪽 바다
    assert.deepEqual(clampCenter({ lat: 30, lng: 126.5 }, KOREA), { lat: 33.1, lng: 126.5 });
  });
});

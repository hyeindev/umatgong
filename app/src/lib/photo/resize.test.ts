/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { fitWithin } from './resize.ts';

describe('fitWithin', () => {
  it('긴 변을 800px로 줄이고 비율을 지킨다', () => {
    assert.deepEqual(fitWithin(4032, 3024, 800), { width: 800, height: 600 });
    assert.deepEqual(fitWithin(3024, 4032, 800), { width: 600, height: 800 });
  });

  it('이미 작은 사진은 키우지 않는다', () => {
    assert.deepEqual(fitWithin(640, 480, 800), { width: 640, height: 480 });
  });

  it('아주 긴 파노라마도 0px이 되지 않는다', () => {
    const { width, height } = fitWithin(20000, 10, 800);
    assert.equal(width, 800);
    assert.ok(height >= 1);
  });
});

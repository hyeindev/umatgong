/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test). Node 타입은 테스트 파일에만 들인다
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import type { VisitPin } from '@/types/visit';

import { toPlacePins } from './pins.ts';

const pin = (
  id: number,
  placeId: number,
  rating: VisitPin['rating'],
  clubColor: VisitPin['clubColor'] = 'SAGE',
): VisitPin => ({
  id,
  placeId,
  coordinate: { lat: 37.29, lng: 127.05 + placeId * 0.001 },
  clubColor,
  rating,
  thumbnailUrl: null,
});

describe('toPlacePins', () => {
  it('「한 번은」은 기본 숨김', () => {
    const places = toPlacePins([pin(1, 1, 'AGAIN'), pin(2, 2, 'OKAY'), pin(3, 3, 'NOPE')], {
      showNope: false,
    });
    assert.deepEqual(
      places.map((p) => p.rating),
      ['AGAIN', 'OKAY'],
    );
  });

  it('필터를 켜면 「한 번은」도 보인다', () => {
    const places = toPlacePins([pin(1, 1, 'AGAIN'), pin(3, 3, 'NOPE')], { showNope: true });
    assert.deepEqual(
      places.map((p) => p.rating),
      ['AGAIN', 'NOPE'],
    );
  });

  it('같은 장소의 기록은 하나로, 가장 좋은 평가와 그 클럽 색을 쓴다', () => {
    const places = toPlacePins(
      [pin(1, 7, 'OKAY', 'SKY'), pin(2, 7, 'AGAIN', 'SAND'), pin(3, 7, 'NOPE', 'LILAC')],
      { showNope: true },
    );
    assert.equal(places.length, 1);
    assert.equal(places[0]?.rating, 'AGAIN');
    assert.equal(places[0]?.clubColor, 'SAND');
    assert.deepEqual(places[0]?.visitIds, [1, 2, 3]);
  });

  it('숨긴 「한 번은」 기록은 묶음에도 들어가지 않는다', () => {
    const places = toPlacePins([pin(1, 7, 'OKAY'), pin(2, 7, 'NOPE')], { showNope: false });
    assert.deepEqual(places[0]?.visitIds, [1]);
  });
});

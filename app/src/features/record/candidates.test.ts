/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import type { Place } from '@/types/place';
import type { Rating, Visit } from '@/types/visit';

import { annotate, rankNearby } from './candidates.ts';

const place = (id: number, distanceMeters: number): Place => ({
  id,
  name: `가게${id}`,
  address: null,
  category: null,
  coordinate: { lat: 37.29, lng: 127.05 },
  distanceMeters,
  clubId: null,
});

const visit = (
  placeId: number,
  authorId: number,
  mine: boolean,
  rating: Rating = 'AGAIN',
): Visit => ({
  id: placeId * 100 + authorId,
  place: { id: placeId, name: '', address: null, category: null, coordinate: { lat: 0, lng: 0 } },
  club: null,
  author: { id: authorId, name: '', avatarUrl: null },
  rating,
  memo: null,
  visibility: 'CLUB',
  visitedAt: '2026-09-30T12:00:00Z',
  createdAt: '2026-09-30T12:00:00Z',
  thumbnailUrl: null,
  thumbnailUrls: [],
  mine,
  distanceMeters: null,
});

describe('rankNearby', () => {
  it('클럽 친구가 기록한 곳 > 내가 기록한 곳 > 거리순', () => {
    const places = [place(1, 10), place(2, 50), place(3, 200), place(4, 30)];
    const visits = [visit(3, 9, false), visit(2, 1, true)];

    assert.deepEqual(
      rankNearby(places, visits).map((c) => c.place.id),
      [3, 2, 1, 4],
    );
  });

  it('같은 묶음 안에서는 가까운 순', () => {
    const places = [place(1, 300), place(2, 100)];
    const visits = [visit(1, 9, false), visit(2, 8, false)];

    assert.deepEqual(
      rankNearby(places, visits).map((c) => c.place.id),
      [2, 1],
    );
  });

  it('나도 가고 친구도 간 곳은 친구 묶음이다', () => {
    const [first] = rankNearby(
      [place(1, 500), place(2, 10)],
      [visit(1, 1, true), visit(1, 9, false)],
    );
    assert.equal(first?.place.id, 1);
    assert.equal(first?.mine, true);
    assert.equal(first?.friendCount, 1);
  });
});

describe('annotate', () => {
  it('친구 수는 사람 수로 세고 가장 좋은 평가를 고른다', () => {
    const [c] = annotate(
      [place(1, 10)],
      [visit(1, 9, false, 'OKAY'), visit(1, 9, false, 'NOPE'), visit(1, 8, false, 'AGAIN')],
    );
    assert.equal(c?.friendCount, 2);
    assert.equal(c?.friendBest, 'AGAIN');
  });

  it('검색 결과 순서는 바꾸지 않는다', () => {
    const result = annotate([place(2, 500), place(1, 10)], [visit(1, 9, false)]);
    assert.deepEqual(
      result.map((c) => c.place.id),
      [2, 1],
    );
  });
});

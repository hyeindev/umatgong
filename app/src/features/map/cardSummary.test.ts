/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import type { Rating, Visit } from '@/types/visit';

import { distanceMeters } from '../../lib/geo/distance.ts';

import { neighborOrder, summarizePlace } from './cardSummary.ts';
import type { PlacePin } from './pins.ts';

const visit = (
  id: number,
  authorId: number,
  rating: Rating,
  extra: { thumb?: string; club?: { id: number; name: string } | null } = {},
): Visit => ({
  id,
  place: {
    id: 7,
    name: '망원동 김반장',
    address: null,
    category: '곱창',
    coordinate: { lat: 37.55, lng: 126.91 },
  },
  club:
    extra.club === undefined
      ? { id: 1, name: '동네친구들', color: 'SAGE' }
      : extra.club && { ...extra.club, color: 'SKY' },
  author: { id: authorId, name: `사람${authorId}`, avatarUrl: null },
  rating,
  memo: null,
  visibility: 'CLUB',
  visitedAt: '2026-09-30T12:00:00Z',
  createdAt: '2026-09-30T12:00:00Z',
  thumbnailUrl: extra.thumb ?? null,
  thumbnailUrls: extra.thumb ? [extra.thumb] : [],
  mine: false,
  distanceMeters: null,
});

describe('summarizePlace', () => {
  it('3명이 모두 또 갈래면 「3명 중 3명이 또 갈래」', () => {
    const s = summarizePlace([visit(1, 1, 'AGAIN'), visit(2, 2, 'AGAIN'), visit(3, 3, 'AGAIN')]);
    assert.equal(s?.headline, '3명 중 3명이 또 갈래');
    assert.equal(s?.people.length, 3);
  });

  it('한 사람이 여러 번 가면 가장 최근 평가 하나로 센다', () => {
    // 최근순: 사람1의 최근 평가는 OKAY
    const s = summarizePlace([visit(1, 1, 'OKAY'), visit(2, 2, 'AGAIN'), visit(3, 1, 'AGAIN')]);
    assert.equal(s?.headline, '2명 중 1명이 또 갈래');
    assert.deepEqual(
      s?.people.map((p) => [p.id, p.rating]),
      [
        [1, 'OKAY'],
        [2, 'AGAIN'],
      ],
    );
  });

  it('또 갈래가 없으면 다음으로 좋은 평가가 대표다', () => {
    assert.equal(
      summarizePlace([visit(1, 1, 'OKAY'), visit(2, 2, 'NOPE')])?.headline,
      '2명 중 1명이 괜찮아',
    );
    assert.equal(summarizePlace([visit(1, 1, 'NOPE')])?.headline, '1명이 한 번은');
  });

  it('썸네일은 가장 최근 것, 클럽은 최근 기록의 클럽이고 다른 클럽 수를 센다', () => {
    const s = summarizePlace([
      visit(1, 1, 'AGAIN'),
      visit(2, 2, 'AGAIN', { thumb: 'https://t/2.jpg', club: { id: 2, name: '회사팀' } }),
      visit(3, 3, 'AGAIN', { thumb: 'https://t/3.jpg' }),
    ]);
    assert.equal(s?.thumbnailUrl, 'https://t/2.jpg');
    assert.equal(s?.club?.name, '동네친구들');
    assert.equal(s?.otherClubCount, 1);
  });

  it('기록이 없으면 null', () => {
    assert.equal(summarizePlace([]), null);
  });
});

describe('neighborOrder', () => {
  const pin = (placeId: number, lat: number, lng: number): PlacePin => ({
    placeId,
    coordinate: { lat, lng },
    rating: 'AGAIN',
    clubColor: 'SAGE',
    visitIds: [placeId],
  });

  it('누른 핀이 먼저, 나머지는 그 핀에서 가까운 순', () => {
    const pins = [
      pin(1, 37.5, 127.0),
      pin(2, 37.6, 127.0),
      pin(3, 37.51, 127.0),
      pin(4, 37.55, 127.0),
    ];
    assert.deepEqual(neighborOrder(pins, 1, distanceMeters), [1, 3, 4, 2]);
  });

  it('개수를 제한한다', () => {
    const pins = Array.from({ length: 30 }, (_, i) => pin(i + 1, 37 + i * 0.001, 127));
    assert.equal(neighborOrder(pins, 1, distanceMeters, 5).length, 5);
  });

  it('누른 핀이 목록에 없으면 그 핀 하나만', () => {
    assert.deepEqual(neighborOrder([pin(1, 37, 127)], 9, distanceMeters), [9]);
  });
});

/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import type { Rating, Visit } from '@/types/visit';

import { formatVisitDate, friendSignal, summarizeDetail } from './detail.ts';

const visit = (
  id: number,
  authorId: number,
  rating: Rating,
  visitedAt: string,
  extra: { mine?: boolean; thumbs?: string[]; clubId?: number } = {},
): Visit => ({
  id,
  place: {
    id: 7,
    name: '망원동 김반장',
    address: '서울 마포구',
    category: '곱창',
    coordinate: { lat: 37.55, lng: 126.91 },
  },
  club: {
    id: extra.clubId ?? 1,
    name: extra.clubId === 2 ? '회사팀' : '동네친구들',
    color: 'SAGE',
  },
  author: { id: authorId, name: `사람${authorId}`, avatarUrl: null },
  rating,
  memo: null,
  visibility: 'CLUB',
  visitedAt,
  createdAt: visitedAt,
  thumbnailUrl: extra.thumbs?.[0] ?? null,
  thumbnailUrls: extra.thumbs ?? [],
  mine: extra.mine ?? false,
  distanceMeters: null,
});

describe('friendSignal', () => {
  it('친구 4명이 모두 또 갈래면 「내 친구 4명 중 4명이 또 갈래」', () => {
    const s = friendSignal(
      [1, 2, 3, 4].map((a) => visit(a, a, 'AGAIN', `2026-09-0${a}T12:00:00Z`)),
    );
    assert.equal(s?.headline, '내 친구 4명 중 4명이 또 갈래');
    assert.equal(s?.people.length, 4);
  });

  it('내 기록은 친구 수에 넣지 않는다', () => {
    const s = friendSignal([
      visit(1, 1, 'AGAIN', '2026-09-01T12:00:00Z', { mine: true }),
      visit(2, 2, 'OKAY', '2026-09-02T12:00:00Z'),
    ]);
    assert.equal(s?.headline, '내 친구 1명 중 1명이 괜찮아');
  });

  it('한 친구가 여러 번 가면 가장 최근 평가로 센다', () => {
    const s = friendSignal([
      visit(1, 2, 'AGAIN', '2026-08-01T12:00:00Z'),
      visit(2, 2, 'NOPE', '2026-09-01T12:00:00Z'),
      visit(3, 3, 'OKAY', '2026-09-02T12:00:00Z'),
    ]);
    assert.equal(s?.headline, '내 친구 2명 중 1명이 괜찮아');
  });

  it('친구 기록이 없으면 null', () => {
    assert.equal(
      friendSignal([visit(1, 1, 'AGAIN', '2026-09-01T12:00:00Z', { mine: true })]),
      null,
    );
  });
});

describe('summarizeDetail', () => {
  it('최근 방문순, 「한 번은」도 남기고, 사진은 촬영자와 함께 모은다', () => {
    const d = summarizeDetail([
      visit(1, 1, 'NOPE', '2026-08-01T12:00:00Z', {
        thumbs: ['https://t/1a.jpg', 'https://t/1b.jpg'],
      }),
      visit(2, 2, 'AGAIN', '2026-09-10T12:00:00Z'),
      visit(3, 3, 'OKAY', '2026-09-05T12:00:00Z', { thumbs: ['https://t/3.jpg'], clubId: 2 }),
    ]);
    assert.deepEqual(
      d?.timeline.map((v) => v.id),
      [2, 3, 1],
    );
    assert.deepEqual(
      d?.photos.map((p) => [p.url, p.author.id]),
      [
        ['https://t/3.jpg', 3],
        ['https://t/1a.jpg', 1],
        ['https://t/1b.jpg', 1],
      ],
    );
    assert.equal(d?.club?.name, '동네친구들');
    assert.equal(d?.otherClubCount, 1);
    assert.equal(d?.myLatest, null);
  });

  it('내 기록이 있으면 가장 최근 것을 myLatest로', () => {
    const d = summarizeDetail([
      visit(1, 9, 'OKAY', '2026-08-01T12:00:00Z', { mine: true }),
      visit(2, 9, 'AGAIN', '2026-09-01T12:00:00Z', { mine: true }),
    ]);
    assert.equal(d?.myLatest?.id, 2);
    assert.equal(d?.signal, null);
  });

  it('기록이 없으면 null', () => {
    assert.equal(summarizeDetail([]), null);
  });
});

describe('formatVisitDate', () => {
  it('올해면 월·일만, 아니면 연도까지', () => {
    const now = new Date('2026-10-04T12:00:00');
    assert.equal(formatVisitDate('2026-08-21T12:00:00', now), '8월 21일');
    assert.equal(formatVisitDate('2025-08-21T12:00:00', now), '2025년 8월 21일');
  });
});

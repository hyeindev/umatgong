// 기록하기 후보 정렬. React·테마에 의존하지 않는 순수 함수만 둔다 (node --test로 돈다).
import type { Place } from '@/types/place';
import type { Rating, Visit } from '@/types/visit';

export type Candidate = {
  place: Place;
  /** 이 가게를 기록한 클럽 친구 수 (나 빼고, 내게 보이는 기록만) */
  friendCount: number;
  /** 친구 기록 중 가장 좋은 평가. 친구 기록이 없으면 null */
  friendBest: Rating | null;
  /** 내가 전에 기록한 곳인지 */
  mine: boolean;
};

const RANK: Record<Rating, number> = { AGAIN: 0, OKAY: 1, NOPE: 2 };

const tier = (c: Candidate) => (c.friendCount > 0 ? 0 : c.mine ? 1 : 2);

/** 후보마다 친구·내 기록을 붙인다. 순서는 바꾸지 않는다 (검색 결과는 정확도 순을 지킨다) */
export const annotate = (places: readonly Place[], visits: readonly Visit[]): Candidate[] => {
  const friends = new Map<number, Set<number>>();
  const best = new Map<number, Rating>();
  const mine = new Set<number>();
  for (const visit of visits) {
    const placeId = visit.place.id;
    if (visit.mine) {
      mine.add(placeId);
      continue;
    }
    const authors = friends.get(placeId) ?? new Set<number>();
    authors.add(visit.author.id);
    friends.set(placeId, authors);
    const current = best.get(placeId);
    if (current === undefined || RANK[visit.rating] < RANK[current]) {
      best.set(placeId, visit.rating);
    }
  }
  return places.map((place) => ({
    place,
    friendCount: friends.get(place.id)?.size ?? 0,
    friendBest: best.get(place.id) ?? null,
    mine: mine.has(place.id),
  }));
};

/**
 * 주변 후보 정렬 (화면기획서 4.5): 클럽 친구가 기록한 곳 > 내가 기록한 곳 > 거리순.
 * 같은 묶음 안에서는 가까운 순. 친구가 갔던 곳을 위에 두면 적중률이 오른다.
 */
export const rankNearby = (places: readonly Place[], visits: readonly Visit[]): Candidate[] =>
  annotate(places, visits).sort(
    (a, b) =>
      tier(a) - tier(b) ||
      (a.place.distanceMeters ?? Number.MAX_SAFE_INTEGER) -
        (b.place.distanceMeters ?? Number.MAX_SAFE_INTEGER),
  );

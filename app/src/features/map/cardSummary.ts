// 미니 카드에 들어갈 요약과 좌우로 넘길 순서. React·테마에 의존하지 않는 순수 함수만 둔다.
import type { ClubColor } from '@/theme/colors';
import type { Coordinate } from '@/types/geo';
import type { Rating, Visit } from '@/types/visit';

import type { PlacePin } from './pins';

const RATING_LABEL: Record<Rating, string> = { AGAIN: '또 갈래', OKAY: '괜찮아', NOPE: '한 번은' };
const RANK: Record<Rating, number> = { AGAIN: 0, OKAY: 1, NOPE: 2 };

export type CardPerson = { id: number; name: string; avatarUrl: string | null; rating: Rating };

export type PlaceSummary = {
  placeId: number;
  name: string;
  category: string | null;
  coordinate: Coordinate;
  /** 가장 최근에 올라온 썸네일 */
  thumbnailUrl: string | null;
  /** 가장 최근 기록의 클럽. 다른 클럽 기록도 있으면 otherClubCount로 센다 */
  club: { name: string; color: ClubColor } | null;
  otherClubCount: number;
  /** 다녀간 사람 (최근순, 한 사람당 한 번). rating은 그 사람의 가장 최근 평가 */
  people: CardPerson[];
  /** 대표 평가: 다녀간 사람 중 가장 좋은 평가 */
  bestRating: Rating;
  /** 대표 평가를 준 사람 수 */
  bestCount: number;
  /** 「3명 중 3명이 또 갈래」 */
  headline: string;
};

/**
 * 장소의 기록들(GET /api/places/{placeId}/visits, 최근 방문순)을 카드 한 장으로 요약한다.
 * 한 사람이 여러 번 갔으면 가장 최근 평가 하나로 센다. 기록이 없으면 null.
 */
export const summarizePlace = (visits: readonly Visit[]): PlaceSummary | null => {
  const [latest] = visits;
  if (!latest) {
    return null;
  }
  const people = new Map<number, CardPerson>();
  for (const visit of visits) {
    if (!people.has(visit.author.id)) {
      people.set(visit.author.id, { ...visit.author, rating: visit.rating });
    }
  }
  const list = [...people.values()];
  const bestRating = list.reduce<Rating>(
    (best, p) => (RANK[p.rating] < RANK[best] ? p.rating : best),
    'NOPE',
  );
  const bestCount = list.filter((p) => p.rating === bestRating).length;
  const label = RATING_LABEL[bestRating];
  const headline =
    list.length === 1 ? `1명이 ${label}` : `${list.length}명 중 ${bestCount}명이 ${label}`;

  const clubIds = new Set(visits.flatMap((v) => (v.club ? [v.club.id] : [])));
  const club = visits.find((v) => v.club)?.club ?? null;

  return {
    placeId: latest.place.id,
    name: latest.place.name,
    category: latest.place.category,
    coordinate: latest.place.coordinate,
    thumbnailUrl: visits.find((v) => v.thumbnailUrl)?.thumbnailUrl ?? null,
    club: club ? { name: club.name, color: club.color } : null,
    otherClubCount: Math.max(0, clubIds.size - 1),
    people: list,
    bestRating,
    bestCount,
    headline,
  };
};

/**
 * 좌우로 넘길 순서. 누른 핀이 맨 앞이고 나머지는 그 핀에서 가까운 순이다.
 * 카드를 여는 순간 한 번 정해 두고, 넘기는 동안 지도가 움직여도 바꾸지 않는다 (순서가 흔들리지 않게).
 */
export const neighborOrder = (
  pins: readonly PlacePin[],
  originPlaceId: number,
  distance: (a: Coordinate, b: Coordinate) => number,
  max = 20,
): number[] => {
  const origin = pins.find((p) => p.placeId === originPlaceId);
  if (!origin) {
    return [originPlaceId];
  }
  const others = pins
    .filter((p) => p.placeId !== originPlaceId)
    .map((p) => ({ id: p.placeId, d: distance(origin.coordinate, p.coordinate) }))
    .sort((a, b) => a.d - b.d)
    .slice(0, max - 1)
    .map((p) => p.id);
  return [originPlaceId, ...others];
};

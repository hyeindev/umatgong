// 장소 상세 요약. React·테마에 의존하지 않는 순수 함수만 둔다 (node --test로 돈다).
import type { ClubColor } from '@/theme/colors';
import type { Rating, Visit } from '@/types/visit';

const RATING_LABEL: Record<Rating, string> = { AGAIN: '또 갈래', OKAY: '괜찮아', NOPE: '한 번은' };
const RANK: Record<Rating, number> = { AGAIN: 0, OKAY: 1, NOPE: 2 };

export type Person = { id: number; name: string; avatarUrl: string | null };

export type GalleryPhoto = {
  url: string;
  visitId: number;
  author: Person;
  visitedAt: string;
};

export type FriendSignal = {
  /** 「내 친구 4명 중 4명이 또 갈래」 */
  headline: string;
  rating: Rating;
  /** 대표 평가를 준 친구들 (아바타 묶음) */
  people: Person[];
  total: number;
  count: number;
};

export type PlaceDetail = {
  place: Visit['place'];
  /** 가장 최근 기록의 클럽. 다른 클럽 기록도 있으면 otherClubCount */
  club: { name: string; color: ClubColor } | null;
  otherClubCount: number;
  /** 친구들이 찍은 사진, 최근 방문부터 */
  photos: GalleryPhoto[];
  /** 친구(나 빼고) 평가 요약. 친구 기록이 없으면 null */
  signal: FriendSignal | null;
  /** 내 가장 최근 기록. 없으면 null — 「나도 갔던 곳」과 평가 수정 진입점 */
  myLatest: Visit | null;
  /** 방문 기록 전체, 최근 방문부터. 「한 번은」도 그대로 둔다 ("가지 마라"도 정보다) */
  timeline: Visit[];
};

const byVisitedDesc = (a: Visit, b: Visit) => b.visitedAt.localeCompare(a.visitedAt) || b.id - a.id;

/**
 * 친구 신호. 한 친구가 여러 번 갔으면 가장 최근 평가 하나로 센다.
 * 대표 평가는 친구들의 평가 중 가장 좋은 것이다.
 */
export const friendSignal = (visits: readonly Visit[]): FriendSignal | null => {
  const latestByFriend = new Map<number, Visit>();
  for (const visit of [...visits].sort(byVisitedDesc)) {
    if (!visit.mine && !latestByFriend.has(visit.author.id)) {
      latestByFriend.set(visit.author.id, visit);
    }
  }
  const friends = [...latestByFriend.values()];
  if (friends.length === 0) {
    return null;
  }
  const rating = friends.reduce<Rating>(
    (best, v) => (RANK[v.rating] < RANK[best] ? v.rating : best),
    'NOPE',
  );
  const people = friends.filter((v) => v.rating === rating).map((v) => v.author);
  return {
    headline: `내 친구 ${friends.length}명 중 ${people.length}명이 ${RATING_LABEL[rating]}`,
    rating,
    people,
    total: friends.length,
    count: people.length,
  };
};

/** GET /api/places/{placeId}/visits 결과를 화면 한 장으로. 기록이 없으면 null */
export const summarizeDetail = (visits: readonly Visit[]): PlaceDetail | null => {
  const timeline = [...visits].sort(byVisitedDesc);
  const [latest] = timeline;
  if (!latest) {
    return null;
  }
  const clubIds = new Set(timeline.flatMap((v) => (v.club ? [v.club.id] : [])));
  const club = timeline.find((v) => v.club)?.club ?? null;
  return {
    place: latest.place,
    club: club ? { name: club.name, color: club.color } : null,
    otherClubCount: Math.max(0, clubIds.size - 1),
    photos: timeline.flatMap((v) =>
      v.thumbnailUrls.map((url) => ({
        url,
        visitId: v.id,
        author: v.author,
        visitedAt: v.visitedAt,
      })),
    ),
    signal: friendSignal(timeline),
    myLatest: timeline.find((v) => v.mine) ?? null,
    timeline,
  };
};

/** 방문 날짜. 올해면 「8월 21일」, 아니면 「2025년 8월 21일」. 기기 시간대 기준 */
export const formatVisitDate = (iso: string, now: Date = new Date()) => {
  const date = new Date(iso);
  const md = `${date.getMonth() + 1}월 ${date.getDate()}일`;
  return date.getFullYear() === now.getFullYear() ? md : `${date.getFullYear()}년 ${md}`;
};

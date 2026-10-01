// 기록 핀 → 지도에 그릴 장소 핀. React·테마에 의존하지 않는 순수 함수만 둔다.
import type { ClubColor } from '@/theme';
import type { Coordinate } from '@/types/geo';
import type { Rating, VisitPin } from '@/types/visit';

/** 지도에 그리는 단위. 같은 장소의 기록은 하나로 묶는다 (api-spec “같은 장소의 기록은 placeId로 묶어 그린다”) */
export type PlacePin = {
  placeId: number;
  coordinate: Coordinate;
  /** 그 장소 기록 중 가장 좋은 평가. 핀 형태가 된다 */
  rating: Rating;
  /** 대표 평가를 남긴 기록의 클럽 색. 클럽 없는 기록이면 null */
  clubColor: ClubColor | null;
  visitIds: number[];
};

const RANK: Record<Rating, number> = { AGAIN: 0, OKAY: 1, NOPE: 2 };

export type PinFilter = {
  /** 「한 번은」은 기본 숨김 (화면기획서 4.1) */
  showNope: boolean;
};

export const toPlacePins = (pins: readonly VisitPin[], filter: PinFilter): PlacePin[] => {
  const byPlace = new Map<number, PlacePin>();
  for (const pin of pins) {
    if (pin.rating === 'NOPE' && !filter.showNope) {
      continue;
    }
    const current = byPlace.get(pin.placeId);
    if (!current) {
      byPlace.set(pin.placeId, {
        placeId: pin.placeId,
        coordinate: pin.coordinate,
        rating: pin.rating,
        clubColor: pin.clubColor,
        visitIds: [pin.id],
      });
      continue;
    }
    current.visitIds.push(pin.id);
    if (RANK[pin.rating] < RANK[current.rating]) {
      current.rating = pin.rating;
      current.clubColor = pin.clubColor;
    }
  }
  return [...byPlace.values()];
};

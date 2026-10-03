import type { MapMarker, MapRegion } from '@/lib/map';
import { colors } from '@/theme';
import type { Rating } from '@/types/visit';

import { clusterByGrid } from './cluster';
import type { PlacePin } from './pins';

const PLACE_PREFIX = 'place:';

const RATING_LABEL: Record<Rating, string> = {
  AGAIN: '또 갈래',
  OKAY: '괜찮아',
  NOPE: '한 번은',
};

/**
 * 평가는 형태, 클럽은 색 (화면기획서 4.1 “핀 규칙”).
 * 「또 갈래」는 클럽과 무관하게 라임 물방울 — 친구가 검증한 곳이라는 신호가 우선이다.
 * 「괜찮아」는 클럽 색 원, 「한 번은」은 흐린 점.
 */
const pinMarker = (place: PlacePin): MapMarker => {
  const base = {
    kind: 'pin' as const,
    id: `${PLACE_PREFIX}${place.placeId}`,
    coordinate: place.coordinate,
    label: `${RATING_LABEL[place.rating]} 기록이 있는 장소`,
  };
  switch (place.rating) {
    case 'AGAIN':
      return { ...base, shape: 'droplet', color: colors.pin.again };
    case 'OKAY':
      // 클럽 없이 남긴 기록은 클럽 색이 없다. 내 위치와 같은 크림으로 그린다
      return {
        ...base,
        shape: 'circle',
        color: place.clubColor ? colors.club[place.clubColor] : colors.surface.cream,
      };
    case 'NOPE':
      return { ...base, shape: 'dot', color: colors.pin.nope };
  }
};

/** 장소 핀을 화면 크기에 맞춰 묶고 지도 표식으로 바꾼다 */
export const toMapMarkers = (places: readonly PlacePin[], region: MapRegion): MapMarker[] =>
  clusterByGrid(places, region).map((entry) => {
    if (entry.kind === 'single') {
      return pinMarker(entry.item);
    }
    return {
      kind: 'cluster',
      id: `cluster:${entry.key}`,
      coordinate: entry.coordinate,
      count: entry.items.length,
      highlighted: entry.items.some((place) => place.rating === 'AGAIN'),
      label: `장소 ${entry.items.length}곳. 눌러서 확대`,
    };
  });

/** 장소 핀이면 그 장소 ID. 묶음(클러스터)이면 null */
export const placeIdOf = (marker: MapMarker): number | null =>
  marker.kind === 'pin' && marker.id.startsWith(PLACE_PREFIX)
    ? Number(marker.id.slice(PLACE_PREFIX.length))
    : null;

/** 미니 카드로 열린 장소의 핀을 크게 그리도록 표시한다 */
export const markSelected = (markers: readonly MapMarker[], placeId: number | null): MapMarker[] =>
  placeId === null
    ? [...markers]
    : markers.map((marker) =>
        marker.kind === 'pin' && marker.id === `${PLACE_PREFIX}${placeId}`
          ? { ...marker, selected: true }
          : marker,
      );

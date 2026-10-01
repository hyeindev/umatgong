import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api';
import type { VisitPin } from '@/types/visit';

import { boundsKey, snapBounds, type Bounds } from './viewport';

export const visitKeys = {
  all: ['visits'] as const,
  /** 격자에 맞춘 영역 + 클럽 필터. 같은 영역을 다시 보면 같은 키라 재요청하지 않는다 */
  map: (bounds: Bounds, clubIds: readonly number[] | null) =>
    [...visitKeys.all, 'map', boundsKey(bounds), clubIds?.join(',') ?? 'all'] as const,
};

// 지도를 오가는 동안 다시 받지 않을 시간. 새 기록을 남기면 그 화면이 visitKeys.all을 무효화한다
const PINS_STALE_MS = 5 * 60_000;
const PINS_CACHE_MS = 30 * 60_000;

/**
 * 화면 영역 안의 기록 핀 (GET /api/visits/map).
 *
 * @param bounds  지도가 멈춘 뒤의 영역. null이면 조회하지 않는다 (지도가 아직 없음)
 * @param clubIds 이 클럽들만. null이면 내 모든 클럽
 */
export const useVisitPins = (bounds: Bounds | null, clubIds: readonly number[] | null) => {
  const snapped = bounds ? snapBounds(bounds) : null;
  return useQuery({
    queryKey: snapped ? visitKeys.map(snapped, clubIds) : [...visitKeys.all, 'map', 'idle'],
    queryFn: () => {
      if (!snapped) {
        throw new Error('bounds가 없으면 조회하지 않는다');
      }
      return apiClient.get<VisitPin[]>('/api/visits/map', {
        query: {
          swLat: snapped.sw.lat,
          swLng: snapped.sw.lng,
          neLat: snapped.ne.lat,
          neLng: snapped.ne.lng,
          clubIds: clubIds?.join(','),
        },
      });
    },
    enabled: snapped !== null,
    staleTime: PINS_STALE_MS,
    gcTime: PINS_CACHE_MS,
    // 새 영역을 받는 동안 이전 핀을 그대로 둔다. 비웠다가 다시 그리면 깜빡인다
    placeholderData: keepPreviousData,
  });
};

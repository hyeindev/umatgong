import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api';
import type { PickedPhoto } from '@/lib/photo';
import type { Coordinate } from '@/types/geo';
import type { CreateCustomPlaceRequest, Place } from '@/types/place';
import type { CreateVisitRequest, ThumbnailUpload, Visit } from '@/types/visit';

/** 후보를 찾는 반경. 먹고 나서 바로 기록하는 경우가 대부분이라 걸어서 몇 분 거리면 된다 */
export const CANDIDATE_RADIUS_METERS = 300;

// 서버 캐시와 같은 기준(소수점 넷째 자리, 약 11m)으로 키를 만든다. 조금 움직여도 같은 후보를 쓴다
const round = (value: number) => Math.round(value * 10_000) / 10_000;
const centerKey = (center: Coordinate | null) =>
  center ? `${round(center.lat)},${round(center.lng)}` : 'none';

export const recordKeys = {
  nearbyPlaces: (center: Coordinate | null) => ['places', 'nearby', centerKey(center)] as const,
  nearbyVisits: (center: Coordinate | null) => ['visits', 'nearby', centerKey(center)] as const,
  search: (query: string, center: Coordinate | null) =>
    ['places', 'search', query, centerKey(center)] as const,
};

/** 기준 좌표 주변 음식점·카페 (카카오 + 내 클럽 전용 장소). 가까운 순 */
export const useNearbyPlaces = (center: Coordinate | null) =>
  useQuery({
    queryKey: recordKeys.nearbyPlaces(center),
    queryFn: () =>
      apiClient.get<Place[]>('/api/places/nearby', {
        query: { lat: center?.lat, lng: center?.lng, radius: CANDIDATE_RADIUS_METERS },
      }),
    enabled: center !== null,
    staleTime: 10 * 60_000,
  });

/** 같은 반경 안의 내게 보이는 기록. 후보 정렬(친구 > 나 > 거리)에만 쓴다 */
export const useNearbyVisits = (center: Coordinate | null) =>
  useQuery({
    queryKey: recordKeys.nearbyVisits(center),
    queryFn: () =>
      apiClient.get<Visit[]>('/api/visits/nearby', {
        query: { lat: center?.lat, lng: center?.lng, radius: CANDIDATE_RADIUS_METERS },
      }),
    enabled: center !== null,
    staleTime: 60_000,
  });

/** 가게명 검색. query는 디바운스한 값을 넘긴다 */
export const useSearchPlaces = (query: string, center: Coordinate | null) => {
  const trimmed = query.trim();
  return useQuery({
    queryKey: recordKeys.search(trimmed.toLowerCase(), center),
    queryFn: () =>
      apiClient.get<Place[]>('/api/places/search', {
        query: { query: trimmed, lat: center?.lat, lng: center?.lng },
      }),
    enabled: trimmed.length > 0,
    staleTime: 10 * 60_000,
  });
};

export const uploadThumbnail = (photo: PickedPhoto) =>
  apiClient.upload<ThumbnailUpload>('/api/photos/thumbnails', photo.toFormData());

/** 고른 장소. 카카오·클럽 전용 장소이거나, 「여기 없어요」로 직접 적은 새 가게 */
export type PlaceChoice =
  | { kind: 'place'; place: Place }
  | { kind: 'new'; name: string; address: string | null; coordinate: Coordinate };

export type SaveVisitInput = {
  choice: PlaceChoice;
  clubId: number | null;
  request: Omit<CreateVisitRequest, 'placeId' | 'clubId'>;
};

/**
 * 저장. 새 가게면 먼저 클럽 전용 장소로 등록하고(클럽이 정해진 뒤라야 등록할 수 있다) 기록을 남긴다.
 * 성공하면 지도 핀·클럽 기록 수가 다시 받아지도록 무효화한다.
 */
export const useSaveVisit = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ choice, clubId, request }: SaveVisitInput) => {
      let placeId: number;
      if (choice.kind === 'place') {
        placeId = choice.place.id;
      } else {
        if (clubId === null) {
          throw new Error('직접 등록하는 가게는 클럽이 있어야 해요.');
        }
        const body: CreateCustomPlaceRequest = {
          clubId,
          name: choice.name,
          address: choice.address,
          coordinate: choice.coordinate,
        };
        placeId = (await apiClient.post<Place>('/api/places/custom', body)).id;
      }
      const visit: CreateVisitRequest = {
        ...request,
        placeId,
        ...(clubId === null ? {} : { clubId }),
      };
      return apiClient.post<Visit>('/api/visits', visit);
    },
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['visits'] }),
        queryClient.invalidateQueries({ queryKey: ['places'] }),
        queryClient.invalidateQueries({ queryKey: ['clubs'] }),
      ]),
  });
};

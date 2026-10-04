import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api';
import type { ScrapStatus } from '@/types/place';
import type { Rating, Visit } from '@/types/visit';

export const placeKeys = {
  scrap: (placeId: number) => ['scraps', 'place', placeId] as const,
};

export const useScrapStatus = (placeId: number) =>
  useQuery({
    queryKey: placeKeys.scrap(placeId),
    queryFn: () => apiClient.get<ScrapStatus>(`/api/places/${placeId}/scrap`),
  });

/** 「가고싶다」 켜고 끄기. 누르는 즉시 화면을 바꾸고, 실패하면 되돌린다 */
export const useToggleScrap = (placeId: number) => {
  const queryClient = useQueryClient();
  const key = placeKeys.scrap(placeId);
  return useMutation({
    mutationFn: (scrap: boolean) =>
      scrap
        ? apiClient.put<ScrapStatus>(`/api/places/${placeId}/scrap`)
        : apiClient.delete<ScrapStatus>(`/api/places/${placeId}/scrap`),
    onMutate: async (scrap) => {
      await queryClient.cancelQueries({ queryKey: key });
      const previous = queryClient.getQueryData<ScrapStatus>(key);
      queryClient.setQueryData<ScrapStatus>(key, { placeId, scrapped: scrap });
      return { previous };
    },
    onError: (_error, _scrap, context) => {
      if (context?.previous) {
        queryClient.setQueryData(key, context.previous);
      }
    },
    onSuccess: (status) => {
      queryClient.setQueryData(key, status);
      void queryClient.invalidateQueries({
        queryKey: ['scraps'],
        exact: false,
        refetchType: 'none',
      });
    },
  });
};

/** 내 평가 고치기 (PATCH /api/visits/{id}). 지도 핀 모양도 바뀌므로 기록 쿼리를 모두 무효화한다 */
export const useUpdateRating = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ visitId, rating }: { visitId: number; rating: Rating }) =>
      apiClient.patch<Visit>(`/api/visits/${visitId}`, { rating }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['visits'] }),
  });
};

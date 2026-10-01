import { useQuery } from '@tanstack/react-query';

import { apiClient } from '@/lib/api';
import type { Club } from '@/types/club';

export const clubKeys = {
  mine: ['clubs', 'mine'] as const,
};

/** 내가 속한 클럽 (GET /api/clubs). 들어간 순서 */
export const useMyClubs = () =>
  useQuery({
    queryKey: clubKeys.mine,
    queryFn: () => apiClient.get<Club[]>('/api/clubs'),
    // 클럽은 가입·탈퇴할 때만 바뀐다. 그때는 그 화면이 이 쿼리를 무효화한다
    staleTime: 10 * 60_000,
  });

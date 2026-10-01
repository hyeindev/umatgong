import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';

import { apiClient } from '@/lib/api';
import type {
  Club,
  ClubInvite,
  ClubMember,
  CreateClubRequest,
  JoinClubRequest,
} from '@/types/club';

export const clubKeys = {
  all: ['clubs'] as const,
  mine: ['clubs', 'mine'] as const,
  members: (clubId: number) => ['clubs', clubId, 'members'] as const,
  invite: (clubId: number) => ['clubs', clubId, 'invite'] as const,
};

// 클럽이 바뀌면 지도 핀도 달라진다 (합류하면 그 클럽 기록이 보이고, 탈퇴하면 사라진다)
const VISITS_KEY = ['visits'] as const;

const refreshAfterMembershipChange = (queryClient: QueryClient) =>
  Promise.all([
    queryClient.invalidateQueries({ queryKey: clubKeys.all }),
    queryClient.invalidateQueries({ queryKey: VISITS_KEY }),
  ]);

/** 내가 속한 클럽 (GET /api/clubs). 들어간 순서 */
export const useMyClubs = () =>
  useQuery({
    queryKey: clubKeys.mine,
    queryFn: () => apiClient.get<Club[]>('/api/clubs'),
    // 클럽은 가입·탈퇴할 때만 바뀐다. 그때는 아래 mutation들이 무효화한다
    staleTime: 10 * 60_000,
  });

/** 한 클럽. 클럽 단건 API가 없으므로 내 클럽 목록에서 찾는다. 멤버가 아니면 undefined */
export const useMyClub = (clubId: number) => {
  const clubs = useMyClubs();
  return { ...clubs, club: clubs.data?.find((club) => club.id === clubId) };
};

export const useClubMembers = (clubId: number) =>
  useQuery({
    queryKey: clubKeys.members(clubId),
    queryFn: () => apiClient.get<ClubMember[]>(`/api/clubs/${clubId}/members`),
  });

/**
 * 지금 초대 코드. 화면을 열 때 미리 받아 둔다. 버튼을 누른 뒤 받으면 웹 클립보드가
 * “사용자가 누른 직후”가 아니라서 복사를 거부할 수 있다.
 * 정원이 다 찬 클럽은 서버가 코드를 주지 않으므로(CLUB_FULL) 부르지 않는다.
 */
export const useClubInvite = (clubId: number, enabled: boolean) =>
  useQuery({
    queryKey: clubKeys.invite(clubId),
    // 본문 없이 부르면 새로 만들지 않고 지금 코드를 준다 (docs/api-spec.md)
    queryFn: () => apiClient.post<ClubInvite>(`/api/clubs/${clubId}/invite`),
    enabled,
    staleTime: 10 * 60_000,
    retry: false,
  });

export const useCreateClub = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateClubRequest) => apiClient.post<Club>('/api/clubs', request),
    onSuccess: () => refreshAfterMembershipChange(queryClient),
  });
};

export const useJoinClub = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: JoinClubRequest) => apiClient.post<Club>('/api/clubs/join', request),
    onSuccess: () => refreshAfterMembershipChange(queryClient),
  });
};

export const useLeaveClub = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (clubId: number) => apiClient.delete<void>(`/api/clubs/${clubId}/members/me`),
    // 갱신을 기다리지 않는다. 기다리면 목록이 먼저 바뀌어 상세 화면이 “없는 클럽”으로 바뀌고,
    // 탈퇴 버튼이 사라지면서 화면 이동이 불리지 않는다. 화면을 먼저 옮기고 갱신은 뒤에서 한다
    onSuccess: () => {
      void refreshAfterMembershipChange(queryClient);
    },
  });
};

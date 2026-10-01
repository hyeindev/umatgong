// docs/api-spec.md 4장 “클럽” 기준.
import type { ClubColor } from '@/theme';

export type Club = {
  id: number;
  name: string;
  /** 색 토큰 이름. 실제 색값은 colors.club[color] */
  color: ClubColor;
  memberCount: number;
  /** 요금제 정원. 숫자를 프론트에 박지 않고 이 값으로 그린다 */
  maxMembers: number;
  full: boolean;
  owner: boolean;
  createdAt: string;
};

/** GET /api/clubs/{clubId}/members 한 건 */
export type ClubMember = {
  userId: number;
  name: string;
  avatarUrl: string | null;
  /** 클럽장인지 */
  owner: boolean;
  joinedAt: string;
};

/** POST /api/clubs/{clubId}/invite 응답 */
export type ClubInvite = {
  inviteCode: string;
  memberCount: number;
  maxMembers: number;
};

export type CreateClubRequest = { name: string };
export type JoinClubRequest = { inviteCode: string };

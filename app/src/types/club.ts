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

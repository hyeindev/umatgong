import type { InviteLinks, PendingInviteStore } from './invite.types';
import { parsePending, serializePending } from './pending';

const KEY = 'umatgong.pending-invite';

// 웹은 지금 열려 있는 주소 기준이다. 미리보기 배포에서 만든 링크는 미리보기로, 운영은 운영으로 간다
export const inviteLinks: InviteLinks = {
  linkFor: (inviteCode) => `${window.location.origin}/invite/${encodeURIComponent(inviteCode)}`,
};

// 카카오 로그인은 같은 탭에서 카카오 페이지를 다녀오므로 localStorage에 둔다
export const pendingInvite: PendingInviteStore = {
  save: async (inviteCode) => {
    try {
      window.localStorage.setItem(KEY, serializePending(inviteCode, Date.now()));
    } catch {
      // 저장소가 막혀 있으면 로그인 뒤 링크를 다시 열어야 한다
    }
  },
  take: async () => {
    try {
      const raw = window.localStorage.getItem(KEY);
      window.localStorage.removeItem(KEY);
      return parsePending(raw, Date.now());
    } catch {
      return null;
    }
  },
};

import { create } from 'zustand';

import type { AuthUser } from '@/types/auth';

/** restoring: 앱 시작 시 저장된 로그인 정보를 확인하는 중 */
export type AuthStatus = 'restoring' | 'signedOut' | 'signedIn';

type AuthState = {
  status: AuthStatus;
  user: AuthUser | null;
  setSignedIn: (user: AuthUser) => void;
  setSignedOut: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  status: 'restoring',
  user: null,
  setSignedIn: (user) => set({ status: 'signedIn', user }),
  setSignedOut: () => set({ status: 'signedOut', user: null }),
}));

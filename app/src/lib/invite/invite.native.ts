import * as Linking from 'expo-linking';
import * as SecureStore from 'expo-secure-store';

import type { InviteLinks, PendingInviteStore } from './invite.types';
import { parsePending, serializePending } from './pending';

const KEY = 'umatgong.pending-invite';
// 앱이 없는 친구도 열 수 있게 웹 주소로 보낸다. 웹 주소가 없으면(개발 빌드) 앱 딥링크로 보낸다
const WEB_URL = process.env.EXPO_PUBLIC_WEB_URL?.replace(/\/+$/, '');

export const inviteLinks: InviteLinks = {
  linkFor: (inviteCode) => {
    const path = `/invite/${encodeURIComponent(inviteCode)}`;
    // createURL은 app.json의 scheme으로 umatgong://invite/... 를 만든다. expo-router가 같은 화면으로 연다
    return WEB_URL ? `${WEB_URL}${path}` : Linking.createURL(path);
  },
};

export const pendingInvite: PendingInviteStore = {
  save: async (inviteCode) => {
    try {
      await SecureStore.setItemAsync(KEY, serializePending(inviteCode, Date.now()));
    } catch {
      // 저장하지 못하면 로그인 뒤 링크를 다시 열어야 한다
    }
  },
  take: async () => {
    try {
      const raw = await SecureStore.getItemAsync(KEY);
      await SecureStore.deleteItemAsync(KEY);
      return parsePending(raw, Date.now());
    } catch {
      return null;
    }
  },
};

import * as SecureStore from 'expo-secure-store';

import { createSerialQueue, parseStoredSession } from './parse';
import type { TokenStorage } from './token.types';

// 네이티브는 SecureStore(iOS 키체인, Android Keystore)에 둔다.
const KEY = 'umatgong.session';

export const tokenStorage: TokenStorage = {
  load: async () => parseStoredSession(await SecureStore.getItemAsync(KEY)),
  save: (session) => SecureStore.setItemAsync(KEY, JSON.stringify(session)),
  clear: () => SecureStore.deleteItemAsync(KEY),
  // 앱 프로세스는 하나뿐이므로 실행 안에서만 순서를 지키면 된다
  exclusive: createSerialQueue(),
};

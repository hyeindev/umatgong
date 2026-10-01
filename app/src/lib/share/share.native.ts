import * as Clipboard from 'expo-clipboard';
import { Share } from 'react-native';

import type { Sharing } from './share.types';

export const sharing: Sharing = {
  copy: async (text) => {
    try {
      await Clipboard.setStringAsync(text);
      return 'copied';
    } catch {
      return 'failed';
    }
  },
  canShare: true,
  share: async ({ message, url }) => {
    try {
      // iOS는 url을 따로 받아 미리보기를 만든다. Android는 message만 쓰므로 링크를 본문에 붙인다
      const result = await Share.share({ message: `${message}\n${url}`, url });
      return result.action === Share.dismissedAction ? 'dismissed' : 'shared';
    } catch {
      return 'failed';
    }
  },
};

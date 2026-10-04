import { Linking } from 'react-native';

import type { Directions } from './directions.types';
import { directionsLinks } from './links';

// 설치된 앱을 먼저 열고, 없으면(열기 실패) 웹 주소로 넘긴다.
// canOpenURL은 iOS에서 스킴 등록(LSApplicationQueriesSchemes)이 필요해 쓰지 않고 바로 열어 본다.
export const directions: Directions = {
  open: async (app, destination) => {
    const links = directionsLinks(app, destination);
    try {
      await Linking.openURL(links.app);
      return true;
    } catch {
      try {
        await Linking.openURL(links.web);
        return true;
      } catch {
        return false;
      }
    }
  },
};

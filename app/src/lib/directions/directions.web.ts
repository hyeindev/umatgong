import type { Directions } from './directions.types';
import { directionsLinks } from './links';

// 웹은 앱 스킴을 직접 열 수 없다(설치 여부를 알 수 없고 실패하면 빈 화면이 된다).
// 지도 서비스의 웹 주소를 새 탭으로 연다. 모바일 브라우저면 그 페이지가 앱 열기를 다시 권한다.
export const directions: Directions = {
  open: async (app, destination) => {
    const opened = window.open(directionsLinks(app, destination).web, '_blank', 'noopener');
    return opened !== null;
  },
};

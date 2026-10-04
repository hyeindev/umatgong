// 외부 지도앱 길찾기 주소. 앱 안에 길찾기를 만들지 않고 카카오맵·네이버지도로 넘긴다 (화면기획서 4.4).
// 순수 함수만 둔다 (node --test로 돈다). REST API 호출이 아니라 사용자가 여는 링크다.
import type { Coordinate } from '@/types/geo';

export type DirectionsApp = 'kakao' | 'naver';

export type Destination = { name: string; coordinate: Coordinate };

export type DirectionsLinks = {
  /** 설치된 앱을 바로 여는 주소 */
  app: string;
  /** 앱이 없거나 웹일 때 여는 주소 (모바일 브라우저에서는 앱 열기를 다시 권한다) */
  web: string;
};

// 네이버지도 앱 스킴은 호출한 앱 식별자를 요구한다
const APP_NAME = 'com.umatgong.app';

export const directionsLinks = (
  app: DirectionsApp,
  { name, coordinate }: Destination,
): DirectionsLinks => {
  const { lat, lng } = coordinate;
  const label = encodeURIComponent(name);
  if (app === 'kakao') {
    return {
      // 출발지를 비우면 앱이 현재 위치에서 시작한다. by: 대중교통
      app: `kakaomap://route?ep=${lat},${lng}&by=PUBLICTRANSIT`,
      // 카카오맵 URL 규칙: /link/to/이름,위도,경도
      web: `https://map.kakao.com/link/to/${label},${lat},${lng}`,
    };
  }
  return {
    app: `nmap://route/public?dlat=${lat}&dlng=${lng}&dname=${label}&appname=${APP_NAME}`,
    // 네이버 모바일 지도 길찾기. 좌표는 ex=경도, ey=위도 (네이버 표기)
    web: `https://m.map.naver.com/route.nhn?menu=route&ename=${label}&ex=${lng}&ey=${lat}&pathType=1`,
  };
};

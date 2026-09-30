/** 좌표는 앱 전체에서 { lat, lng }로 통일한다. 카카오의 x/y 표기는 백엔드 안에만 있다. */
export type Coordinate = {
  lat: number;
  lng: number;
};

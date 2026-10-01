import type { ReactElement, Ref } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

import type { Coordinate } from '@/types/geo';

export type MapBounds = {
  /** 남서쪽 끝 */
  sw: Coordinate;
  /** 북동쪽 끝 */
  ne: Coordinate;
};

/**
 * 지도 확대 단계. 지도 SDK의 레벨 숫자는 구현 안에만 둔다.
 * docs/화면기획서.md 4.1 “줌 단계”: 전국 → 광역 → 동네
 */
export type MapZoom = 'nation' | 'region' | 'neighborhood';

export type MapHandle = {
  moveTo: (center: Coordinate, zoom: MapZoom) => void;
};

export type MapProps = {
  /** 처음 보여줄 영역. 이 영역이 화면에 꽉 차게 맞춘다 */
  initialBounds: MapBounds;
  /** 내 위치. 크림색 점 + 옅은 후광으로 그린다. 없으면 그리지 않는다 */
  myLocation?: Coordinate | null;
  /** 지도를 불러오지 못했을 때 (키 누락, SDK 로드 실패 등) */
  onError?: (message: string) => void;
  style?: StyleProp<ViewStyle>;
  ref?: Ref<MapHandle>;
};

/** 웹(카카오 JS SDK)과 네이티브(카카오 네이티브 SDK)가 함께 지키는 인터페이스 */
export type MapComponent = (props: MapProps) => ReactElement | null;

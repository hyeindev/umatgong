import type { ReactElement, Ref } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

import type { Coordinate } from '@/types/geo';

export type MapBounds = {
  /** 남서쪽 끝 */
  sw: Coordinate;
  /** 북동쪽 끝 */
  ne: Coordinate;
};

/** 지도가 지금 보여주는 영역과 화면 크기(픽셀). 클러스터링이 확대 정도를 여기서 계산한다 */
export type MapRegion = {
  bounds: MapBounds;
  width: number;
  height: number;
};

/**
 * 지도 확대 단계. 지도 SDK의 레벨 숫자는 구현 안에만 둔다.
 * docs/화면기획서.md 4.1 “줌 단계”: 전국 → 광역 → 동네
 */
export type MapZoom = 'nation' | 'region' | 'neighborhood';

/**
 * 지도 위에 그릴 표식. 지도는 도메인(평가·클럽)을 모른다. 모양과 색만 받는다.
 * - droplet: 물방울 핀 (아래 끝이 좌표)
 * - circle: 테두리 있는 원
 * - dot: 작은 점
 */
export type MapPinShape = 'droplet' | 'circle' | 'dot';

export type MapMarker =
  | {
      kind: 'pin';
      /** 같은 표식이면 같은 id. 바뀐 것만 다시 그린다 */
      id: string;
      coordinate: Coordinate;
      shape: MapPinShape;
      color: string;
      /** 스크린 리더용 이름 */
      label: string;
    }
  | {
      kind: 'cluster';
      id: string;
      coordinate: Coordinate;
      count: number;
      /** 강조 테두리를 두를지 (「또 갈래」가 들어 있는 묶음) */
      highlighted: boolean;
      label: string;
    };

export type MapHandle = {
  /** 지도가 아직 준비되지 않았으면 준비된 뒤 옮긴다 */
  moveTo: (center: Coordinate, zoom: MapZoom) => void;
  /** 한 단계 이상 확대하며 그 좌표로 옮긴다 (클러스터를 눌렀을 때) */
  zoomInAt: (center: Coordinate) => void;
};

export type MapProps = {
  /** 처음 보여줄 영역. 이 영역이 화면에 꽉 차게 맞춘다 */
  initialBounds: MapBounds;
  /**
   * 지도를 이 영역 밖으로 옮기지 못하게 하고, 이 영역 전체가 화면에 들어오는 것보다 멀리 축소하지 못하게 한다.
   * 남한만 보이게 할 때 쓴다 (주변 나라로 끌려가지 않게)
   */
  restrictTo?: MapBounds;
  /** 내 위치. 크림색 점 + 옅은 후광으로 그린다. 없으면 그리지 않는다 */
  myLocation?: Coordinate | null;
  markers?: readonly MapMarker[];
  onMarkerPress?: (marker: MapMarker) => void;
  /** 지도 빈 곳을 눌렀을 때 그 좌표 (표식을 누르면 부르지 않는다) */
  onPress?: (coordinate: Coordinate) => void;
  /**
   * 지도 이동·확대가 **멈췄을 때만** 부른다. 움직이는 동안 매 프레임 부르지 않는다.
   * 처음 지도를 그린 뒤에도 한 번 부른다
   */
  onRegionChange?: (region: MapRegion) => void;
  /** 지도를 불러오지 못했을 때 (키 누락, SDK 로드 실패 등) */
  onError?: (message: string) => void;
  style?: StyleProp<ViewStyle>;
  ref?: Ref<MapHandle>;
};

/** 웹(카카오 JS SDK)과 네이티브(카카오 네이티브 SDK)가 함께 지키는 인터페이스 */
export type MapComponent = (props: MapProps) => ReactElement | null;

import type { ReactElement } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

import type { Coordinate } from '@/types/geo';

export type MapProps = {
  center: Coordinate;
  style?: StyleProp<ViewStyle>;
};

/** 웹(카카오 JS SDK)과 네이티브(카카오 네이티브 SDK)가 함께 지키는 인터페이스 */
export type MapComponent = (props: MapProps) => ReactElement | null;

import type { DirectionsApp, Destination } from './links';

export type Directions = {
  /** 외부 지도앱으로 길찾기를 연다. 열지 못하면 false */
  open: (app: DirectionsApp, destination: Destination) => Promise<boolean>;
};

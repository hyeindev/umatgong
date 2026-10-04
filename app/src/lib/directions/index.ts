// 번들러가 플랫폼에 맞춰 directions.web.ts / directions.native.ts 중 하나를 고른다.
export { directions } from './directions';
export type { Directions } from './directions.types';
export { directionsLinks, type Destination, type DirectionsApp } from './links';

// 번들러가 플랫폼에 맞춰 scanner.web.ts / scanner.native.ts 중 하나를 고른다.
export { photoScanner } from './scanner';
export type { PhotoScanner, ScannedPhoto, ScanResult } from './scanner.types';

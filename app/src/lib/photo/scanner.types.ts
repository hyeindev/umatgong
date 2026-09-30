import type { Coordinate } from '@/types/geo';

export type ScannedPhoto = {
  id: string;
  /** ISO 8601 UTC */
  takenAt: string;
  coordinate: Coordinate | null;
};

/**
 * 웹에는 사진첩 API가 없다. 결함이 아니라 설계이므로 에러 대신 supported: false를 돌려주고,
 * 화면은 이 값을 보고 앱 설치를 안내한다.
 */
export type ScanResult = { supported: false } | { supported: true; photos: ScannedPhoto[] };

export type PhotoScanner = {
  isSupported: boolean;
  scan: () => Promise<ScanResult>;
};

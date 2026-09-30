import type { PhotoScanner } from './scanner.types';

export const photoScanner: PhotoScanner = {
  isSupported: true,
  // TODO: 사진첩 스캔을 구현한다. 아직 구현 전이므로 호출 즉시 실패시킨다.
  scan: async () => {
    throw new Error('photoScanner.scan is not implemented yet');
  },
};

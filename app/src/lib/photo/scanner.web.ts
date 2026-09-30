import type { PhotoScanner } from './scanner.types';

export const photoScanner: PhotoScanner = {
  isSupported: false,
  scan: async () => ({ supported: false }),
};

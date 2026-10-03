import type { LocationProvider } from './location.types';

// TODO: expo-location으로 구현한다 (네이티브 지도와 함께).
export const location: LocationProvider = {
  getCurrent: async () => ({ status: 'unavailable' }),
  peek: async () => null,
};

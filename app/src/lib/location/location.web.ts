import type { LocationProvider } from './location.types';

export const location: LocationProvider = {
  getCurrent: () =>
    new Promise((resolve) => {
      if (typeof navigator === 'undefined' || !navigator.geolocation) {
        resolve({ status: 'unavailable' });
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) =>
          resolve({
            status: 'granted',
            coordinate: { lat: position.coords.latitude, lng: position.coords.longitude },
          }),
        (error) =>
          resolve({ status: error.code === error.PERMISSION_DENIED ? 'denied' : 'unavailable' }),
        // 지도 이동용이라 1분 안의 위치면 충분하다
        { enableHighAccuracy: false, timeout: 8000, maximumAge: 60_000 },
      );
    }),
};

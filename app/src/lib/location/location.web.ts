import type { LocationProvider } from './location.types';

const getCurrent: LocationProvider['getCurrent'] = () =>
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
  });

export const location: LocationProvider = {
  getCurrent,
  peek: async () => {
    // 권한 상태를 물어볼 수 없는 브라우저(Safari 일부)에서는 묻지 않고 넘어간다
    const permissions = typeof navigator !== 'undefined' ? navigator.permissions : undefined;
    if (!permissions) {
      return null;
    }
    try {
      const status = await permissions.query({ name: 'geolocation' });
      if (status.state !== 'granted') {
        return null;
      }
    } catch {
      return null;
    }
    const result = await getCurrent();
    return result.status === 'granted' ? result.coordinate : null;
  },
};

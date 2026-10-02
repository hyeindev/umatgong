// 카카오맵 JS SDK. 로그인용 SDK(lib/kakao, window.Kakao)와는 다른 스크립트다 (window.kakao.maps).
// 키는 로그인과 같은 JavaScript 키를 쓴다. 카카오 콘솔에서 카카오맵 사용 설정과 웹 도메인 등록이 필요하다.
const JS_KEY = process.env.EXPO_PUBLIC_KAKAO_JS_KEY;

/** 쓰는 기능만 적은 최소 타입 */
export type KakaoLatLng = { getLat: () => number; getLng: () => number };

export type KakaoMouseEvent = { latLng: KakaoLatLng };

export type KakaoLatLngBounds = {
  getSouthWest: () => KakaoLatLng;
  getNorthEast: () => KakaoLatLng;
};

export type KakaoMap = {
  setBounds: (bounds: unknown) => void;
  getBounds: () => KakaoLatLngBounds;
  getLevel: () => number;
  getCenter: () => KakaoLatLng;
  /** 이보다 멀리 축소하지 못하게 한다 (레벨이 클수록 멀다) */
  setMaxLevel: (level: number) => void;
  setCenter: (latLng: KakaoLatLng) => void;
  setLevel: (level: number, options?: { animate?: boolean }) => void;
  panTo: (latLng: KakaoLatLng) => void;
  relayout: () => void;
};

export type KakaoCustomOverlay = {
  setMap: (map: KakaoMap | null) => void;
  setPosition: (latLng: KakaoLatLng) => void;
};

export type KakaoMaps = {
  load: (callback: () => void) => void;
  LatLng: new (lat: number, lng: number) => KakaoLatLng;
  LatLngBounds: new (sw: KakaoLatLng, ne: KakaoLatLng) => KakaoLatLngBounds;
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => KakaoMap;
  CustomOverlay: new (options: {
    position: KakaoLatLng;
    content: HTMLElement;
    xAnchor?: number;
    yAnchor?: number;
    zIndex?: number;
    /** true면 오버레이를 눌러도 지도의 클릭·드래그가 시작되지 않는다 */
    clickable?: boolean;
  }) => KakaoCustomOverlay;
  event: {
    addListener: {
      (target: KakaoMap, type: 'idle', handler: () => void): void;
      (target: KakaoMap, type: 'click', handler: (event: KakaoMouseEvent) => void): void;
    };
    removeListener: {
      (target: KakaoMap, type: 'idle', handler: () => void): void;
      (target: KakaoMap, type: 'click', handler: (event: KakaoMouseEvent) => void): void;
    };
  };
};

declare global {
  interface Window {
    kakao?: { maps?: KakaoMaps };
  }
}

let loading: Promise<KakaoMaps> | null = null;

/** SDK를 한 번만 불러온다 */
export const loadKakaoMaps = () => {
  loading ??= new Promise<KakaoMaps>((resolve, reject) => {
    if (!JS_KEY) {
      reject(new Error('EXPO_PUBLIC_KAKAO_JS_KEY가 설정되지 않았습니다.'));
      return;
    }
    const ready = () => {
      const maps = window.kakao?.maps;
      if (!maps) {
        reject(new Error('카카오맵을 불러오지 못했습니다.'));
        return;
      }
      maps.load(() => resolve(maps));
    };
    if (window.kakao?.maps) {
      ready();
      return;
    }
    const script = document.createElement('script');
    // autoload=false: 스크립트가 받아진 뒤 maps.load로 직접 초기화한다
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(JS_KEY)}&autoload=false`;
    script.async = true;
    script.onload = ready;
    script.onerror = () =>
      reject(
        new Error(
          '카카오맵을 불러오지 못했습니다. 카카오 콘솔의 도메인·카카오맵 설정을 확인해 주세요.',
        ),
      );
    document.head.appendChild(script);
  }).catch((error: unknown) => {
    // 실패하면 다음 시도에서 다시 불러온다
    loading = null;
    throw error;
  });
  return loading;
};

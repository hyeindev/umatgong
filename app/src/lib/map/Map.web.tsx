import { useEffect, useImperativeHandle, useRef } from 'react';
import { View } from 'react-native';

import { colors } from '@/theme';

import {
  loadKakaoMaps,
  type KakaoCustomOverlay,
  type KakaoMap,
  type KakaoMaps,
} from './kakaoMapSdk.web';
import type { MapComponent, MapZoom } from './Map.types';

// 카카오맵 레벨: 1이 가장 가깝고 14가 가장 멀다. docs/화면기획서.md 4.1 줌 단계와 맞춘다
const LEVEL: Record<MapZoom, number> = {
  nation: 13,
  region: 9,
  neighborhood: 5,
};

// 카카오맵에는 어두운 지도 스타일이 없다. 타일을 반전해 디자인 시스템의 어두운 지도처럼 만든다.
// invert와 hue-rotate(180°)는 두 번 걸면 원래대로 돌아오므로, 지도 위에 그리는 요소(내 위치 점 등)에는
// 같은 필터를 한 번 더 걸어 원래 색으로 보이게 한다.
const DARK_TILES = 'invert(1) hue-rotate(180deg)';
const DIM_TILES = 'brightness(0.82) contrast(0.92) saturate(0.7)';

const myLocationDot = () => {
  const dot = document.createElement('div');
  Object.assign(dot.style, {
    width: '16px',
    height: '16px',
    borderRadius: '50%',
    background: colors.pin.me,
    boxShadow: `0 0 0 8px ${colors.pin.meHalo}`,
    filter: DARK_TILES,
  });
  dot.setAttribute('aria-label', '내 위치');
  return dot;
};

export const Map: MapComponent = ({ initialBounds, myLocation, onError, style, ref }) => {
  const containerRef = useRef<View>(null);
  const mapsRef = useRef<KakaoMaps | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const meRef = useRef<KakaoCustomOverlay | null>(null);
  // 지도 생성은 처음 한 번만. 이후 바뀌는 값은 아래 effect들이 반영한다
  const initial = useRef({ initialBounds, onError });

  useEffect(() => {
    let cancelled = false;
    loadKakaoMaps()
      .then((maps) => {
        // react-native-web의 View ref는 DOM 요소다
        const container = containerRef.current as unknown as HTMLElement | null;
        if (cancelled || !container) {
          return;
        }
        container.style.filter = `${DARK_TILES} ${DIM_TILES}`;
        const { sw, ne } = initial.current.initialBounds;
        const map = new maps.Map(container, {
          center: new maps.LatLng((sw.lat + ne.lat) / 2, (sw.lng + ne.lng) / 2),
          level: LEVEL.nation,
        });
        map.setBounds(
          new maps.LatLngBounds(new maps.LatLng(sw.lat, sw.lng), new maps.LatLng(ne.lat, ne.lng)),
        );
        mapsRef.current = maps;
        mapRef.current = map;
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          initial.current.onError?.(
            error instanceof Error ? error.message : '지도를 불러오지 못했어요.',
          );
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const maps = mapsRef.current;
    const map = mapRef.current;
    if (!maps || !map) {
      return;
    }
    if (!myLocation) {
      meRef.current?.setMap(null);
      meRef.current = null;
      return;
    }
    const position = new maps.LatLng(myLocation.lat, myLocation.lng);
    if (meRef.current) {
      meRef.current.setPosition(position);
    } else {
      meRef.current = new maps.CustomOverlay({ position, content: myLocationDot(), zIndex: 10 });
      meRef.current.setMap(map);
    }
  }, [myLocation]);

  useImperativeHandle(
    ref,
    () => ({
      moveTo: (center, zoom) => {
        const maps = mapsRef.current;
        const map = mapRef.current;
        if (!maps || !map) {
          return;
        }
        map.setLevel(LEVEL[zoom]);
        map.panTo(new maps.LatLng(center.lat, center.lng));
      },
    }),
    [],
  );

  return <View ref={containerRef} style={style} />;
};

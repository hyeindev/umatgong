import { useEffect, useImperativeHandle, useRef, useState } from 'react';
import { View } from 'react-native';

import {
  loadKakaoMaps,
  type KakaoCustomOverlay,
  type KakaoMap,
  type KakaoMaps,
} from './kakaoMapSdk.web';
import type { MapComponent, MapMarker, MapRegion, MapZoom } from './Map.types';
import { anchorOf, DARK_TILES, markerElement, myLocationElement, zIndexOf } from './markers.web';

// 카카오맵 레벨: 1이 가장 가깝고 14가 가장 멀다. docs/화면기획서.md 4.1 줌 단계와 맞춘다
const LEVEL: Record<MapZoom, number> = {
  nation: 13,
  region: 9,
  neighborhood: 5,
};
const MIN_LEVEL = 1;
// 클러스터를 누르면 이만큼 확대한다. 한 단계씩이면 묶음이 거의 그대로라 여러 번 눌러야 한다
const CLUSTER_ZOOM_STEP = 2;

// 카카오맵에는 어두운 지도 스타일이 없다. 타일을 반전해 디자인 시스템의 어두운 지도처럼 만든다.
// 지도 위 요소는 markers.web.ts에서 같은 필터를 한 번 더 걸어 원래 색으로 보이게 한다.
const DIM_TILES = 'brightness(0.82) contrast(0.92) saturate(0.7)';

// 같은 id라도 모양·색·개수가 바뀌면 다시 그린다
const signatureOf = (marker: MapMarker) => JSON.stringify(marker);

type DrawnMarker = { overlay: KakaoCustomOverlay; signature: string };

export const Map: MapComponent = ({
  initialBounds,
  myLocation,
  markers,
  onMarkerPress,
  onRegionChange,
  onError,
  style,
  ref,
}) => {
  const containerRef = useRef<View>(null);
  const mapsRef = useRef<KakaoMaps | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const meRef = useRef<KakaoCustomOverlay | null>(null);
  const drawnRef = useRef(new globalThis.Map<string, DrawnMarker>());
  // SDK를 받기 전에 들어온 마커·내 위치도 지도가 준비되면 그리도록 effect가 이 값을 본다
  const [ready, setReady] = useState(false);
  // 지도 생성은 처음 한 번만. 이후 바뀌는 값은 아래 effect들이 반영한다
  const initial = useRef({ initialBounds, onError });
  // 이벤트 핸들러는 지도를 만들 때 한 번만 등록하므로 최신 콜백은 ref로 읽는다
  const callbacks = useRef({ onMarkerPress, onRegionChange });
  useEffect(() => {
    callbacks.current = { onMarkerPress, onRegionChange };
  }, [onMarkerPress, onRegionChange]);

  useEffect(() => {
    let cancelled = false;
    let cleanup: (() => void) | undefined;
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

        // idle은 이동·확대 애니메이션이 끝났을 때 한 번만 온다. 드래그 중에는 오지 않는다
        const emitRegion = () => {
          const bounds = map.getBounds();
          const southWest = bounds.getSouthWest();
          const northEast = bounds.getNorthEast();
          const region: MapRegion = {
            bounds: {
              sw: { lat: southWest.getLat(), lng: southWest.getLng() },
              ne: { lat: northEast.getLat(), lng: northEast.getLng() },
            },
            width: container.clientWidth,
            height: container.clientHeight,
          };
          callbacks.current.onRegionChange?.(region);
        };
        maps.event.addListener(map, 'idle', emitRegion);
        cleanup = () => maps.event.removeListener(map, 'idle', emitRegion);

        mapsRef.current = maps;
        mapRef.current = map;
        setReady(true);
        // setBounds의 idle이 리스너 등록보다 먼저 지나갔을 수 있으므로 첫 영역은 직접 알린다
        emitRegion();
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
      cleanup?.();
    };
  }, []);

  useEffect(() => {
    const maps = mapsRef.current;
    const map = mapRef.current;
    if (!ready || !maps || !map) {
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
      meRef.current = new maps.CustomOverlay({
        position,
        content: myLocationElement(),
        zIndex: 10,
      });
      meRef.current.setMap(map);
    }
  }, [ready, myLocation]);

  // 바뀐 표식만 지우고 다시 그린다. 매번 전부 지우면 지도를 옮길 때마다 핀이 깜빡인다
  useEffect(() => {
    const maps = mapsRef.current;
    const map = mapRef.current;
    if (!ready || !maps || !map) {
      return;
    }
    const drawn = drawnRef.current;
    const next = new globalThis.Map((markers ?? []).map((marker) => [marker.id, marker]));

    drawn.forEach((entry, id) => {
      const marker = next.get(id);
      if (!marker || signatureOf(marker) !== entry.signature) {
        entry.overlay.setMap(null);
        drawn.delete(id);
      }
    });
    next.forEach((marker, id) => {
      if (drawn.has(id)) {
        return;
      }
      const anchor = anchorOf(marker);
      const overlay = new maps.CustomOverlay({
        position: new maps.LatLng(marker.coordinate.lat, marker.coordinate.lng),
        content: markerElement(marker, () => callbacks.current.onMarkerPress?.(marker)),
        xAnchor: anchor.x,
        yAnchor: anchor.y,
        zIndex: zIndexOf(marker),
        clickable: true,
      });
      overlay.setMap(map);
      drawn.set(id, { overlay, signature: signatureOf(marker) });
    });
  }, [ready, markers]);

  // 화면을 떠나면 지도 위 표식을 모두 걷는다
  useEffect(() => {
    const drawn = drawnRef.current;
    return () => {
      drawn.forEach((entry) => entry.overlay.setMap(null));
      drawn.clear();
    };
  }, []);

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
      zoomInAt: (center) => {
        const maps = mapsRef.current;
        const map = mapRef.current;
        if (!maps || !map) {
          return;
        }
        map.setLevel(Math.max(MIN_LEVEL, map.getLevel() - CLUSTER_ZOOM_STEP));
        map.panTo(new maps.LatLng(center.lat, center.lng));
      },
    }),
    [],
  );

  return <View ref={containerRef} style={style} />;
};

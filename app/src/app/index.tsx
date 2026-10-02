import { useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Avatar } from '@/components/Avatar';
import { useMyClubs } from '@/features/club';
import {
  EmptyStateSheet,
  MapFilterChips,
  SOUTH_KOREA_BOUNDS,
  toMapMarkers,
  toPlacePins,
  useDebouncedValue,
  useVisitPins,
} from '@/features/map';
import { pendingInvite } from '@/lib/invite';
import { location } from '@/lib/location';
import { Map, type MapHandle, type MapMarker, type MapRegion } from '@/lib/map';
import { useAuthStore } from '@/stores/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';
import type { Coordinate } from '@/types/geo';

// 지도가 멈춘 뒤 이만큼 더 움직이지 않으면 조회한다 (화면기획서 4.1 “성능”: 디바운스 300ms 이상)
const FETCH_DEBOUNCE_MS = 300;

// 지도 홈. 앱을 켜면 바로 여기다 (화면기획서 1장 “지도가 곧 홈이다”).
// 첫 진입은 남한 전체에서 시작하고, 확대하면 동네로 들어간다 (화면기획서 4.1, ref-main.html M1).
//
// TODO: 핀을 누르면 미니 카드(4.2), 「내 근처 맛집」(4.3). 「내 근처 맛집」이 생기면 「기록하기」는 그 위의 보조 버튼이 된다.
// TODO: 전국·광역 단계의 지역명 클러스터는 서버 지역 집계 API가 생기면 바꾼다. 지금은 화면 격자로 묶는다.
export default function MapHomeScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const user = useAuthStore((state) => state.user);
  const mapRef = useRef<MapHandle>(null);
  const [myLocation, setMyLocation] = useState<Coordinate | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);

  // 기록하기에서 저장하고 돌아오면 새 핀이 있는 동네로 옮긴다. recorded는 같은 곳을 다시 기록해도 바뀌는 값이다
  const { focusLat, focusLng, recorded } = useLocalSearchParams<{
    focusLat?: string;
    focusLng?: string;
    recorded?: string;
  }>();
  // 안내는 상태로 따로 두지 않고 recorded 값에서 정한다. 몇 초 뒤 그 값을 “본 것”으로 표시해 끈다
  const [seenRecorded, setSeenRecorded] = useState<string | null>(null);
  useEffect(() => {
    const lat = Number(focusLat);
    const lng = Number(focusLng);
    if (!recorded || !Number.isFinite(lat) || !Number.isFinite(lng)) {
      return;
    }
    mapRef.current?.moveTo({ lat, lng }, 'neighborhood');
    const timer = setTimeout(() => setSeenRecorded(recorded), 3000);
    return () => clearTimeout(timer);
  }, [focusLat, focusLng, recorded]);
  const recordedNotice = recorded && recorded !== seenRecorded ? '새 핀을 꽂았어요.' : null;

  // 로그인 전에 초대 링크를 열었다면 로그인을 마치고 여기로 온다. 맡겨 둔 코드로 합류 화면을 다시 연다
  useEffect(() => {
    let active = true;
    pendingInvite.take().then((code) => {
      if (active && code) {
        router.push(`/invite/${code}`);
      }
    });
    return () => {
      active = false;
    };
  }, [router]);

  // 지도는 멈췄을 때만 영역을 알린다. 클러스터는 바로 다시 묶고, 조회는 디바운스한 영역으로 한다
  const [region, setRegion] = useState<MapRegion | null>(null);
  const settledRegion = useDebouncedValue(region, FETCH_DEBOUNCE_MS);

  const myClubs = useMyClubs().data;
  const clubs = useMemo(() => myClubs ?? [], [myClubs]);
  const [pickedClubIds, setPickedClubIds] = useState<number[] | null>(null);
  const [showNope, setShowNope] = useState(false);

  // 탈퇴한 클럽 ID는 버린다. 전부 켜져 있으면 null(필터 없음)로 보내 같은 캐시 키를 쓴다
  const clubIds = useMemo(() => {
    if (pickedClubIds === null || clubs.length < 2) {
      return null;
    }
    const valid = clubs.map((club) => club.id).filter((id) => pickedClubIds.includes(id));
    return valid.length === 0 || valid.length === clubs.length ? null : valid.sort((a, b) => a - b);
  }, [clubs, pickedClubIds]);

  const pins = useVisitPins(settledRegion?.bounds ?? null, clubIds);
  // 빈 상태는 “내 클럽 전체에 기록이 하나도 없을 때”다. 지금 보는 영역이 비었다고 띄우지 않는다
  const nationwide = useVisitPins(SOUTH_KOREA_BOUNDS, null);
  const isEmpty = nationwide.isSuccess && nationwide.data.length === 0;

  const markers = useMemo(() => {
    if (!region || !pins.data) {
      return [];
    }
    return toMapMarkers(toPlacePins(pins.data, { showNope }), region);
  }, [region, pins.data, showNope]);

  const toggleClub = (clubId: number) => {
    setPickedClubIds((previous) => {
      const all = clubs.map((club) => club.id);
      const current = previous ?? all;
      const next = current.includes(clubId)
        ? current.filter((id) => id !== clubId)
        : [...current, clubId];
      // 다 끄면 지도가 비어 버린다. 마지막 하나는 켜 둔다
      if (next.length === 0) {
        return previous;
      }
      return next.length === all.length ? null : next;
    });
  };

  const onMarkerPress = useCallback((marker: MapMarker) => {
    if (marker.kind === 'cluster') {
      mapRef.current?.zoomInAt(marker.coordinate);
    }
  }, []);

  const goToMyLocation = async () => {
    setLocating(true);
    setMessage(null);
    const result = await location.getCurrent();
    setLocating(false);
    if (result.status === 'granted') {
      setMyLocation(result.coordinate);
      mapRef.current?.moveTo(result.coordinate, 'neighborhood');
      return;
    }
    setMessage(
      result.status === 'denied'
        ? '위치 권한을 허용하면 내 주변으로 이동해요.'
        : '지금은 위치를 확인할 수 없어요.',
    );
  };

  const notice =
    message ??
    recordedNotice ??
    (pins.isError ? '기록을 불러오지 못했어요. 지도를 움직이면 다시 시도해요.' : null);

  return (
    <View style={styles.screen}>
      <Map
        ref={mapRef}
        initialBounds={SOUTH_KOREA_BOUNDS}
        myLocation={myLocation}
        markers={markers}
        onMarkerPress={onMarkerPress}
        onRegionChange={setRegion}
        onError={setMessage}
        style={StyleSheet.absoluteFill}
      />

      <View style={[styles.topBar, { top: insets.top + spacing[3] }]} pointerEvents="box-none">
        {isEmpty ? (
          <View style={styles.filters} />
        ) : (
          <View style={styles.filters}>
            <MapFilterChips
              clubs={clubs}
              selectedClubIds={clubIds}
              onToggleClub={toggleClub}
              showNope={showNope}
              onToggleNope={() => setShowNope((value) => !value)}
            />
          </View>
        )}
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="프로필"
          onPress={() => router.push('/profile')}
          style={({ pressed }) => [styles.profile, pressed && styles.pressed]}
        >
          {user ? <Avatar uri={user.avatarUrl} name={user.name} size={size.avatar.sm} /> : null}
        </Pressable>
      </View>

      {notice ? (
        <View
          style={[
            styles.message,
            { top: insets.top + spacing[3] + size.floatingButton + spacing[3] },
          ]}
        >
          <Text style={styles.messageLabel} accessibilityRole="alert">
            {notice}
          </Text>
        </View>
      ) : null}

      {/* 아래에서부터: 빈 상태 시트(기록이 없을 때만), 그 바로 위 우측에 현재 위치 버튼 */}
      <View
        style={[styles.bottom, !isEmpty && { paddingBottom: insets.bottom + spacing[6] }]}
        pointerEvents="box-none"
      >
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="현재 위치로 이동"
          accessibilityState={{ busy: locating }}
          onPress={goToMyLocation}
          style={({ pressed }) => [styles.locate, pressed && styles.pressed]}
        >
          <Svg width={18} height={18} viewBox="0 0 18 18">
            <Circle
              cx={9}
              cy={9}
              r={6.5}
              stroke={colors.text.primary}
              strokeWidth={2}
              fill="none"
            />
            <Circle
              cx={9}
              cy={9}
              r={2.5}
              fill={locating ? colors.accent.default : colors.text.primary}
            />
          </Svg>
        </Pressable>
        {isEmpty ? (
          <EmptyStateSheet onRecord={() => router.push('/record')} />
        ) : (
          <Pressable
            accessibilityRole="button"
            onPress={() => router.push('/record')}
            style={({ pressed }) => [styles.record, pressed && styles.pressed]}
          >
            <Text style={styles.recordLabel}>기록하기</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
  },
  topBar: {
    position: 'absolute',
    left: layout.screenGutter,
    right: layout.screenGutter,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[3],
  },
  filters: {
    flex: 1,
    flexDirection: 'row',
  },
  profile: {
    width: size.floatingButton,
    height: size.floatingButton,
    borderRadius: shape.pill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.sheet,
    borderWidth: 2,
    borderColor: colors.surface.cream,
    overflow: 'hidden',
  },
  message: {
    position: 'absolute',
    left: layout.screenGutter,
    right: layout.screenGutter,
    padding: spacing[3],
    borderRadius: shape.button,
    backgroundColor: colors.surface.floating,
  },
  messageLabel: {
    ...textStyles.bodySmall,
    color: colors.text.primary,
  },
  bottom: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    gap: spacing[3],
  },
  locate: {
    alignSelf: 'flex-end',
    marginRight: layout.screenGutter,
    width: size.floatingButton,
    height: size.floatingButton,
    borderRadius: shape.button,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.sheet,
    borderWidth: 1,
    borderColor: colors.border.subtle,
  },
  // 화면의 주요 행동 하나라 라임 (한 화면에 라임 면 2개까지)
  record: {
    marginHorizontal: layout.screenGutter,
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  recordLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  pressed: {
    opacity: 0.85,
  },
});

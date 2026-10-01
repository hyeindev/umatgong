import { useRouter } from 'expo-router';
import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Avatar } from '@/components/Avatar';
import { EmptyStateSheet, SOUTH_KOREA_BOUNDS } from '@/features/map';
import { location } from '@/lib/location';
import { Map, type MapHandle } from '@/lib/map';
import { useAuthStore } from '@/stores/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';
import type { Coordinate } from '@/types/geo';

// 지도 홈. 앱을 켜면 바로 여기다 (화면기획서 1장 “지도가 곧 홈이다”).
// 첫 진입은 남한 전체에서 시작하고, 확대하면 동네로 들어간다 (화면기획서 4.1, ref-main.html M1).
//
// TODO: 클럽·기록 API가 생기면 핀·클러스터·클럽 칩·「내 근처 맛집」을 붙인다.
// 지금은 기록을 조회할 API가 없으므로 항상 빈 상태를 보여준다.
export default function MapHomeScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const user = useAuthStore((state) => state.user);
  const mapRef = useRef<MapHandle>(null);
  const [myLocation, setMyLocation] = useState<Coordinate | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);

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

  return (
    <View style={styles.screen}>
      <Map
        ref={mapRef}
        initialBounds={SOUTH_KOREA_BOUNDS}
        myLocation={myLocation}
        onError={setMessage}
        style={StyleSheet.absoluteFill}
      />

      <View style={[styles.topBar, { top: insets.top + spacing[3] }]} pointerEvents="box-none">
        {/* 상단 좌측 클럽 칩은 클럽이 2개 이상일 때만 (화면기획서 4.1). 클럽 API 이후에 붙인다 */}
        <View />
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="프로필"
          onPress={() => router.push('/profile')}
          style={({ pressed }) => [styles.profile, pressed && styles.pressed]}
        >
          {user ? <Avatar uri={user.avatarUrl} name={user.name} size={size.avatar.sm} /> : null}
        </Pressable>
      </View>

      {message ? (
        <View
          style={[
            styles.message,
            { top: insets.top + spacing[3] + size.floatingButton + spacing[3] },
          ]}
        >
          <Text style={styles.messageLabel} accessibilityRole="alert">
            {message}
          </Text>
        </View>
      ) : null}

      {/* 아래에서부터: 빈 상태 시트, 그 바로 위 우측에 현재 위치 버튼 */}
      <View style={styles.bottom} pointerEvents="box-none">
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
        <EmptyStateSheet />
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
  pressed: {
    opacity: 0.85,
  },
});

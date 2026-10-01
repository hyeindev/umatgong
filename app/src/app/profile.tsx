import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '@/components/Avatar';
import { signOut } from '@/features/auth';
import { useAuthStore } from '@/stores/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// 프로필. 지금은 로그인 상태 확인과 로그아웃만 있다 (내 지도·클럽·설정은 이후).
export default function ProfileScreen() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const logout = useMutation({ mutationFn: signOut });

  if (!user) {
    return null;
  }

  return (
    <SafeAreaView style={styles.screen}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="지도로 돌아가기"
        onPress={() => (router.canGoBack() ? router.back() : router.replace('/'))}
        style={({ pressed }) => [styles.back, pressed && styles.pressed]}
      >
        <Text style={styles.backLabel}>지도</Text>
      </Pressable>
      <View style={styles.profile}>
        <Avatar uri={user.avatarUrl} name={user.name} size={size.avatar.lg} />
        <Text style={styles.name}>{user.name}</Text>
        <View style={styles.badge}>
          <Text style={styles.badgeLabel}>로그인됨</Text>
        </View>
      </View>

      <Pressable
        accessibilityRole="button"
        accessibilityState={{ busy: logout.isPending, disabled: logout.isPending }}
        disabled={logout.isPending}
        onPress={() => logout.mutate()}
        style={({ pressed }) => [styles.logout, pressed && styles.pressed]}
      >
        {logout.isPending ? (
          <ActivityIndicator color={colors.text.primary} />
        ) : (
          <Text style={styles.logoutLabel}>로그아웃</Text>
        )}
      </Pressable>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[8],
  },
  back: {
    alignSelf: 'flex-start',
    marginTop: spacing[3],
    paddingVertical: spacing[2.5],
    paddingHorizontal: spacing[3.5],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  backLabel: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  profile: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[4],
  },
  name: {
    ...textStyles.display,
    color: colors.text.primary,
  },
  badge: {
    paddingVertical: spacing[2],
    paddingHorizontal: spacing[3],
    borderRadius: shape.pill,
    backgroundColor: colors.accent.default,
  },
  badgeLabel: {
    ...textStyles.label,
    color: colors.accent.on,
  },
  logout: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.surface.raised,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.85,
  },
  logoutLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
});

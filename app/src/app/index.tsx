import { useMutation } from '@tanstack/react-query';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Avatar } from '@/components/Avatar';
import { signOut } from '@/features/auth';
import { useAuthStore } from '@/stores/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// 임시 메인 화면. 로그인 → 토큰 저장 → 복원이 이어지는지 확인하는 용도다. 지도는 여기에 만들지 않는다.
export default function MainScreen() {
  const user = useAuthStore((state) => state.user);
  const logout = useMutation({ mutationFn: signOut });

  if (!user) {
    return null;
  }

  return (
    <SafeAreaView style={styles.screen}>
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

import { useRouter, type Href } from 'expo-router';
import { Pressable, StyleSheet, Text } from 'react-native';

import { colors, shape, spacing, textStyles } from '@/theme';

type Props = {
  label: string;
  /** 뒤로 갈 화면이 없을 때(링크로 바로 들어왔을 때) 갈 곳 */
  fallback: Href;
};

/** 화면 왼쪽 위 되돌아가기. 뒤로 가기는 항상 지도로 수렴한다 (화면기획서 5장) */
export function BackButton({ label, fallback }: Props) {
  const router = useRouter();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${label}(으)로 돌아가기`}
      onPress={() => (router.canGoBack() ? router.back() : router.replace(fallback))}
      style={({ pressed }) => [styles.back, pressed && styles.pressed]}
    >
      <Text style={styles.label}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  back: {
    alignSelf: 'flex-start',
    paddingVertical: spacing[2.5],
    paddingHorizontal: spacing[3.5],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  pressed: {
    opacity: 0.85,
  },
  label: {
    ...textStyles.label,
    color: colors.text.primary,
  },
});

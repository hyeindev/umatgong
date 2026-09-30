import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { colors, radius, size, spacing, textStyles } from '@/theme';

type Props = {
  onPress: () => void;
  loading?: boolean;
};

// 카카오 로그인 버튼 디자인 가이드: 노란 바탕, 말풍선 심볼, “카카오 로그인” 문구
export function KakaoLoginButton({ onPress, loading = false }: Props) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel="카카오 로그인"
      accessibilityState={{ busy: loading, disabled: loading }}
      disabled={loading}
      onPress={onPress}
      style={({ pressed }) => [styles.button, pressed && styles.pressed]}
    >
      <View style={styles.symbol}>
        <Svg width={20} height={20} viewBox="0 0 18 18">
          <Path
            d="M9 1.5C4.3 1.5.5 4.5.5 8.2c0 2.4 1.6 4.5 4 5.7l-.9 3.4c-.1.3.3.5.5.3l4-2.7c.3 0 .6.1.9.1 4.7 0 8.5-3 8.5-6.8S13.7 1.5 9 1.5z"
            fill={colors.brand.kakaoSymbol}
          />
        </Svg>
      </View>
      {loading ? (
        <ActivityIndicator color={colors.brand.onKakao} />
      ) : (
        <Text style={styles.label}>카카오 로그인</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    height: size.button,
    borderRadius: radius.md,
    backgroundColor: colors.brand.kakao,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.85,
  },
  symbol: {
    position: 'absolute',
    left: spacing[5],
  },
  label: {
    ...textStyles.button,
    color: colors.brand.onKakao,
  },
});

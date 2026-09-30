import { useMutation } from '@tanstack/react-query';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { StickerCard } from '@/components/StickerCard';
import { KakaoLoginButton, loginErrorMessage, signInWithKakao } from '@/features/auth';
import {
  colors,
  fontFamily,
  layout,
  letterSpacing,
  lineHeight,
  shape,
  spacing,
  textStyles,
} from '@/theme';

// 로그인 화면에만 나오는 큰 제목. 스케일 밖의 크기라 여기서 직접 지정한다.
// 스티커 카드 콜라주의 크기·위치·각도도 이 화면만의 그림이라 직접 지정한다.
const HEADLINE_SIZE = 42;
const HEADLINE_INITIAL_SIZE = 52;

export default function LoginScreen() {
  const signIn = useMutation({ mutationFn: signInWithKakao });

  return (
    <SafeAreaView style={styles.screen}>
      <View
        style={styles.collage}
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
      >
        <StickerCard photoHeight={196} style={[styles.card, styles.cardLeft]} />
        <StickerCard photoHeight={190} style={[styles.card, styles.cardRight]} />
        <StickerCard
          photoHeight={196}
          style={[styles.card, styles.cardFront]}
          sticker={
            <View style={styles.sticker}>
              <Text style={styles.stickerLabel}>또 갈래</Text>
            </View>
          }
        />
      </View>

      <View style={styles.copy}>
        <Text style={styles.headline} accessibilityRole="header">
          <Text style={styles.initial}>우</Text>리끼리{'\n'}
          <Text style={styles.initial}>맛</Text>집{'\n'}
          <Text style={styles.initial}>공</Text>유
        </Text>
        <Text style={styles.tagline}>친구가 또 가고 싶다고 한 곳만, 지도에.</Text>
      </View>

      <View style={styles.actions}>
        {signIn.isError ? (
          <Text style={styles.error} accessibilityRole="alert">
            {loginErrorMessage(signIn.error)}
          </Text>
        ) : null}
        <KakaoLoginButton onPress={() => signIn.mutate()} loading={signIn.isPending} />
        <Text style={styles.terms}>시작하면 이용약관과 개인정보 처리방침에 동의하게 됩니다.</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
  },
  collage: {
    flex: 1,
    minHeight: 360,
    overflow: 'visible',
  },
  card: {
    position: 'absolute',
  },
  cardLeft: {
    left: spacing[3.5],
    top: spacing[8],
    width: 180,
    transform: [{ rotate: '-8deg' }],
  },
  cardRight: {
    right: spacing[4],
    top: spacing[3],
    width: 176,
    transform: [{ rotate: '7deg' }],
  },
  cardFront: {
    alignSelf: 'center',
    top: 128,
    width: 212,
    transform: [{ rotate: '-2deg' }],
  },
  sticker: {
    paddingVertical: spacing[2],
    paddingHorizontal: spacing[3],
    borderRadius: shape.pill,
    backgroundColor: colors.accent.default,
    transform: [{ rotate: '6deg' }],
  },
  stickerLabel: {
    ...textStyles.label,
    color: colors.accent.on,
  },
  copy: {
    paddingHorizontal: spacing[7],
    paddingBottom: spacing[6],
    gap: spacing[3],
  },
  headline: {
    fontFamily: fontFamily.semibold,
    fontSize: HEADLINE_SIZE,
    lineHeight: Math.round(HEADLINE_SIZE * lineHeight.tight),
    letterSpacing: HEADLINE_SIZE * letterSpacing.tighter,
    color: colors.text.primary,
  },
  initial: {
    fontFamily: fontFamily.heavy,
    fontSize: HEADLINE_INITIAL_SIZE,
    color: colors.accent.default,
  },
  tagline: {
    ...textStyles.body,
    color: colors.text.secondary,
  },
  actions: {
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[8],
    gap: spacing[3.5],
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.status.danger,
    textAlign: 'center',
  },
  terms: {
    ...textStyles.bodySmall,
    color: colors.text.tertiary,
    textAlign: 'center',
  },
});

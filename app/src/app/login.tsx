import { useMutation } from '@tanstack/react-query';
import { StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';
import { SafeAreaView } from 'react-native-safe-area-context';

import { StickerCard } from '@/components/StickerCard';
import { KakaoLoginButton, loginErrorMessage, signInWithKakao } from '@/features/auth';
import { loginPhotos } from '@/features/auth/loginPhotos';
import {
  colors,
  fontFamily,
  layout,
  letterSpacing,
  lineHeight,
  shape,
  size,
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
      {/* 장식용 콜라주. 스크린리더는 건너뛴다 */}
      <View
        style={styles.collage}
        aria-hidden
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
      >
        <StickerCard
          photo={loginPhotos[0]}
          photoHeight={196}
          style={[styles.card, styles.cardLeft]}
        />
        <StickerCard
          photo={loginPhotos[1]}
          photoHeight={190}
          style={[styles.card, styles.cardRight]}
        />
        <StickerCard
          photo={loginPhotos[2]}
          photoHeight={196}
          style={[styles.card, styles.cardFront]}
          caption={<ExampleCaption />}
          sticker={
            <View style={styles.sticker}>
              <PinIcon />
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

// 앞 카드의 예시 캡션: 가게 이름 + 겹친 아바타 + 검증 문장
function ExampleCaption() {
  return (
    <View style={styles.caption}>
      <Text style={styles.captionTitle} numberOfLines={1}>
        망원 파스타바
      </Text>
      <View style={styles.captionMeta}>
        <View style={styles.avatars}>
          {colors.placeholderAvatar.onCream.map((color, index) => (
            <View
              key={color}
              style={[styles.avatar, { backgroundColor: color }, index > 0 && styles.avatarOverlap]}
            />
          ))}
        </View>
        <Text style={styles.captionVerified}>3명 모두 또 갈래</Text>
      </View>
    </View>
  );
}

// “또 갈래” 물방울 핀 (디자인 시스템 v2 지도 핀과 같은 모양)
function PinIcon() {
  return (
    <Svg width={13} height={17} viewBox="0 0 26 34">
      <Path
        d="M13 1.5C6.6 1.5 1.5 6.6 1.5 13c0 8.3 11.5 19.5 11.5 19.5S24.5 21.3 24.5 13C24.5 6.6 19.4 1.5 13 1.5z"
        fill={colors.accent.on}
      />
      <Circle cx={13} cy={13} r={4.5} fill={colors.accent.default} />
    </Svg>
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
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
    paddingVertical: spacing[2],
    paddingLeft: spacing[2],
    paddingRight: spacing[3],
    borderRadius: shape.pill,
    backgroundColor: colors.accent.default,
    transform: [{ rotate: '6deg' }],
  },
  stickerLabel: {
    ...textStyles.label,
    color: colors.accent.on,
  },
  caption: {
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[1],
    gap: spacing[1.5],
  },
  captionTitle: {
    ...textStyles.itemTitle,
    fontFamily: fontFamily.semibold,
    color: colors.text.onCream.primary,
  },
  captionMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  avatars: {
    flexDirection: 'row',
  },
  avatar: {
    width: size.avatar.xs,
    height: size.avatar.xs,
    borderRadius: shape.pill,
    borderWidth: 1.5,
    borderColor: colors.surface.cream,
  },
  avatarOverlap: {
    marginLeft: -spacing[2],
  },
  captionVerified: {
    ...textStyles.caption,
    fontFamily: fontFamily.medium,
    color: colors.text.onCream.again,
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

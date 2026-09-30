import type { ReactNode } from 'react';
import {
  Image,
  StyleSheet,
  View,
  type ImageSourcePropType,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

import { colors, radius, shape, spacing } from '@/theme';

type Props = {
  /** 크림 테두리 안의 사진 자리 높이 */
  photoHeight: number;
  /** 사진. 없으면 회색 자리만 보인다 */
  photo?: ImageSourcePropType | null;
  /** 사진 아래 캡션. 없으면 폴라로이드처럼 아래 여백만 둔다 */
  caption?: ReactNode;
  /** 카드 모서리에 걸쳐 붙는 스티커 */
  sticker?: ReactNode;
  style?: StyleProp<ViewStyle>;
};

/**
 * 스티커(폴라로이드) 카드. 디자인 시스템 v2 “기록은 스티커처럼”.
 * 회전·위치는 쓰는 쪽에서 style로 준다 (−8° ~ +8°).
 */
export function StickerCard({ photoHeight, photo, caption, sticker, style }: Props) {
  return (
    <View style={[styles.card, style]}>
      <View style={[styles.photo, { height: photoHeight }]}>
        {photo ? (
          <Image
            source={photo}
            // 웹은 크기를 주지 않으면 원본 크기로 그려져 왼쪽 위만 보인다. 자리를 꽉 채운다
            style={styles.image}
            resizeMode="cover"
            // 사진은 장식이다. 스크린리더가 읽지 않는다
            accessible={false}
            importantForAccessibility="no"
          />
        ) : null}
      </View>
      {caption ?? <View style={styles.bottom} />}
      {sticker ? <View style={styles.sticker}>{sticker}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    padding: spacing[2],
    paddingBottom: 0,
    borderRadius: shape.stickerCard,
    backgroundColor: colors.surface.cream,
    shadowColor: colors.shadow,
    shadowOpacity: 0.6,
    shadowRadius: spacing[6],
    shadowOffset: { width: 0, height: spacing[4] },
    elevation: 12,
  },
  photo: {
    borderRadius: radius.sm,
    overflow: 'hidden',
    backgroundColor: colors.surface.placeholder,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  bottom: {
    height: spacing[7],
  },
  sticker: {
    position: 'absolute',
    right: -spacing[4],
    top: -spacing[4],
  },
});

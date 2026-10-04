import { StyleSheet, Text, View } from 'react-native';

import { colors, shape, spacing, textStyles } from '@/theme';
import type { Rating } from '@/types/visit';

const LABEL: Record<Rating, string> = { AGAIN: '또 갈래', OKAY: '괜찮아', NOPE: '한 번은' };

/** 평가 표시. 또 갈래는 라임, 괜찮아는 크림, 한 번은은 테두리만 — 「한 번은」도 숨기지 않는다 */
export function RatingChip({ rating }: { rating: Rating }) {
  return (
    <View style={[styles.chip, CHIP[rating]]}>
      <Text style={[styles.label, rating === 'NOPE' && styles.labelNope]}>{LABEL[rating]}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: {
    paddingVertical: spacing[1.5],
    paddingHorizontal: spacing[2.5],
    borderRadius: shape.pill,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  again: {
    backgroundColor: colors.accent.default,
  },
  okay: {
    backgroundColor: colors.surface.cream,
  },
  nope: {
    borderColor: colors.border.strong,
  },
  label: {
    ...textStyles.caption,
    color: colors.accent.on,
  },
  labelNope: {
    color: colors.text.secondary,
  },
});

const CHIP: Record<Rating, object> = { AGAIN: styles.again, OKAY: styles.okay, NOPE: styles.nope };

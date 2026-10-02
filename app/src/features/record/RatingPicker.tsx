import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, shape, size, spacing, textStyles } from '@/theme';
import type { Rating } from '@/types/visit';

import { RATING_HINT, RATING_LABEL, RATINGS } from './ratings';

type Props = {
  value: Rating | null;
  onChange: (rating: Rating) => void;
};

/**
 * 평가 3단계. 한 손으로 누르게 화면 하단에 크게 둔다 (화면기획서 4.5 “한 손 조작”).
 * 고른 버튼만 채운다 — 또 갈래는 라임, 괜찮아는 크림, 한 번은은 테두리만 진하게.
 */
export function RatingPicker({ value, onChange }: Props) {
  return (
    <View style={styles.row} accessibilityRole="radiogroup" accessibilityLabel="평가">
      {RATINGS.map((rating) => {
        const selected = value === rating;
        return (
          <Pressable
            key={rating}
            accessibilityRole="radio"
            accessibilityState={{ checked: selected }}
            accessibilityLabel={`${RATING_LABEL[rating]}, ${RATING_HINT[rating]}`}
            onPress={() => onChange(rating)}
            style={({ pressed }) => [
              styles.button,
              selected && SELECTED[rating],
              pressed && styles.pressed,
            ]}
          >
            <Text style={[styles.label, selected && LABEL_SELECTED[rating]]}>
              {RATING_LABEL[rating]}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  button: {
    flex: 1,
    height: size.ratingButton,
    borderRadius: shape.button,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.raised,
    borderWidth: 2,
    borderColor: colors.surface.raised,
  },
  pressed: {
    opacity: 0.85,
  },
  label: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  again: {
    backgroundColor: colors.accent.default,
    borderColor: colors.accent.default,
  },
  okay: {
    backgroundColor: colors.surface.cream,
    borderColor: colors.surface.cream,
  },
  nope: {
    borderColor: colors.border.strong,
  },
  labelOnFilled: {
    color: colors.accent.on,
  },
});

const SELECTED: Record<Rating, object> = {
  AGAIN: styles.again,
  OKAY: styles.okay,
  NOPE: styles.nope,
};
const LABEL_SELECTED: Record<Rating, object | undefined> = {
  AGAIN: styles.labelOnFilled,
  OKAY: styles.labelOnFilled,
  NOPE: undefined,
};

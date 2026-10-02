import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, shape, size, spacing, textStyles } from '@/theme';
import type { Club } from '@/types/club';

type Props = {
  clubs: readonly Club[];
  value: number | null;
  onChange: (clubId: number) => void;
};

/** 어느 클럽에 남길지. 클럽이 2개 이상일 때만 화면에 나온다 (1개면 묻지 않고 자동 선택) */
export function ClubChoice({ clubs, value, onChange }: Props) {
  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>어느 클럽에 남길까요?</Text>
      <View style={styles.row}>
        {clubs.map((club) => {
          const on = club.id === value;
          return (
            <Pressable
              key={club.id}
              accessibilityRole="radio"
              accessibilityState={{ checked: on }}
              accessibilityLabel={club.name}
              onPress={() => onChange(club.id)}
              style={({ pressed }) => [styles.chip, on && styles.chipOn, pressed && styles.pressed]}
            >
              <View style={[styles.dot, { backgroundColor: colors.club[club.color] }]} />
              <Text style={[styles.chipLabel, on && styles.chipLabelOn]} numberOfLines={1}>
                {club.name}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: spacing[2],
  },
  label: {
    ...textStyles.sectionLabel,
    color: colors.text.secondary,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing[1.5],
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
    paddingVertical: spacing[2.5],
    paddingHorizontal: spacing[3],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  chipOn: {
    backgroundColor: colors.surface.cream,
  },
  pressed: {
    opacity: 0.85,
  },
  dot: {
    width: size.chipDot,
    height: size.chipDot,
    borderRadius: shape.pill,
  },
  chipLabel: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  chipLabelOn: {
    color: colors.text.onCream.primary,
  },
});

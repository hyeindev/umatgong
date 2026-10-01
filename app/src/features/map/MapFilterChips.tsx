import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { colors, layout, shape, size, spacing, textStyles } from '@/theme';
import type { Club } from '@/types/club';

type Props = {
  /** 2개 이상일 때만 클럽 칩을 보여준다 (화면기획서 4.1). 그보다 적으면 넘겨도 그리지 않는다 */
  clubs: readonly Club[];
  /** 켜진 클럽. null이면 전부 켜짐 */
  selectedClubIds: readonly number[] | null;
  onToggleClub: (clubId: number) => void;
  showNope: boolean;
  onToggleNope: () => void;
};

/**
 * 지도 상단 필터. 누르면 바로 반영한다 (적용 버튼 없음, 화면기획서 5장).
 * 켜진 칩은 크림 바탕 (시안 ref-main.html .chip.on).
 */
export function MapFilterChips({
  clubs,
  selectedClubIds,
  onToggleClub,
  showNope,
  onToggleNope,
}: Props) {
  const showClubs = clubs.length >= 2;
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.row}
      style={styles.scroll}
    >
      {showClubs
        ? clubs.map((club) => {
            const on = selectedClubIds === null || selectedClubIds.includes(club.id);
            return (
              <Chip
                key={club.id}
                label={club.name}
                on={on}
                dotColor={colors.club[club.color]}
                onPress={() => onToggleClub(club.id)}
              />
            );
          })
        : null}
      {/* 흐린 점 색은 크림 바탕(켜짐) 위에서 보이지 않으므로 켜지면 잉크 계열로 바꾼다 */}
      <Chip
        label="한 번은"
        on={showNope}
        dotColor={showNope ? colors.text.onCream.tertiary : colors.pin.nope}
        onPress={onToggleNope}
      />
    </ScrollView>
  );
}

type ChipProps = {
  label: string;
  on: boolean;
  dotColor: string;
  onPress: () => void;
};

function Chip({ label, on, dotColor, onPress }: ChipProps) {
  return (
    <Pressable
      accessibilityRole="switch"
      accessibilityState={{ checked: on }}
      accessibilityLabel={label}
      onPress={onPress}
      style={({ pressed }) => [styles.chip, on && styles.chipOn, pressed && styles.pressed]}
    >
      <View style={[styles.dot, { backgroundColor: dotColor }]} />
      <Text style={[styles.label, on && styles.labelOn]} numberOfLines={1}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  scroll: {
    flexGrow: 0,
    flexShrink: 1,
  },
  row: {
    gap: spacing[1.5],
    paddingRight: layout.screenGutter,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
    paddingVertical: spacing[2.5],
    paddingHorizontal: spacing[3],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.floating,
  },
  chipOn: {
    backgroundColor: colors.surface.cream,
  },
  dot: {
    width: size.chipDot,
    height: size.chipDot,
    borderRadius: shape.pill,
  },
  label: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  labelOn: {
    color: colors.text.onCream.primary,
  },
  pressed: {
    opacity: 0.85,
  },
});

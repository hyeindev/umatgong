import { StyleSheet, Text, View } from 'react-native';

import { colors, layout, shape, spacing, textStyles } from '@/theme';

/**
 * 정원이 다 찼을 때. “기존 멤버는 그대로, 초대만 막힘”을 분명히 보여준다 (화면기획서 4.8, 시안 D3 하단 카드).
 * 빨강(status.danger)은 정원 초과 표시에만 쓴다.
 */
export function ClubFullNotice() {
  return (
    <View style={styles.card} accessibilityRole="alert">
      <View style={styles.badge}>
        <Text style={styles.badgeLabel}>정원 꽉 참</Text>
      </View>
      <Text style={styles.body}>
        지금 멤버는 그대로 쓸 수 있어요. <Text style={styles.strong}>새 초대만 막혀요.</Text>
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: spacing[2.5],
    padding: layout.cardPadding,
    borderRadius: shape.card,
    backgroundColor: colors.surface.cream,
  },
  badge: {
    alignSelf: 'flex-start',
    paddingVertical: spacing[1],
    paddingHorizontal: spacing[2],
    borderRadius: shape.pill,
    backgroundColor: colors.status.danger,
  },
  badgeLabel: {
    ...textStyles.caption,
    color: colors.status.onDanger,
  },
  body: {
    ...textStyles.bodySmall,
    color: colors.text.onCream.secondary,
  },
  strong: {
    fontFamily: textStyles.label.fontFamily,
    color: colors.text.onCream.primary,
  },
});

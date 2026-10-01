import { StyleSheet, Text, View } from 'react-native';

import { colors, shape, size, spacing, textStyles } from '@/theme';

type Props = {
  memberCount: number;
  maxMembers: number;
};

// 정원이 이보다 많으면 칸 대신 한 줄 막대로 그린다 (유료 정원 30명을 칸으로 그리면 너무 잘다)
const MAX_SEGMENTS = 12;

/** “8명 중 5명”과 정원 막대 (시안 ref-all.html D3) */
export function ClubCapacity({ memberCount, maxMembers }: Props) {
  const filled = Math.min(memberCount, maxMembers);
  return (
    <View style={styles.wrap}>
      <View style={styles.header}>
        <Text style={styles.count} accessibilityLabel={`정원 ${maxMembers}명 중 ${memberCount}명`}>
          {maxMembers}명 중 <Text style={styles.countAccent}>{memberCount}명</Text>
        </Text>
        <Text style={styles.limit}>정원 {maxMembers}명</Text>
      </View>
      {maxMembers <= MAX_SEGMENTS ? (
        <View style={styles.segments} aria-hidden>
          {Array.from({ length: maxMembers }, (_, index) => (
            <View key={index} style={[styles.segment, index < filled ? styles.on : styles.off]} />
          ))}
        </View>
      ) : (
        <View style={[styles.segment, styles.off, styles.track]} aria-hidden>
          <View style={[styles.segment, styles.on, { width: `${(filled / maxMembers) * 100}%` }]} />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: spacing[4],
  },
  header: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    gap: spacing[3],
  },
  count: {
    ...textStyles.title,
    color: colors.text.primary,
  },
  countAccent: {
    color: colors.accent.default,
  },
  limit: {
    ...textStyles.meta,
    color: colors.text.tertiary,
  },
  segments: {
    flexDirection: 'row',
    gap: spacing[1.5],
  },
  segment: {
    flex: 1,
    height: size.capacityBar,
    borderRadius: shape.pill,
  },
  // 칸 하나하나는 라임 “면”이 아니라 표시라서 라임 2개 규칙과 별개로 시안처럼 라임으로 채운다
  on: {
    backgroundColor: colors.accent.default,
  },
  off: {
    backgroundColor: colors.surface.raised,
  },
  track: {
    flex: undefined,
    overflow: 'hidden',
  },
});

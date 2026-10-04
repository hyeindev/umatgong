import { StyleSheet, Text, View } from 'react-native';

import { Avatar } from '@/components/Avatar';
import { colors, shape, size, spacing, textStyles } from '@/theme';

import type { FriendSignal } from './detail';

const MAX_AVATARS = 5;

/**
 * 핵심 신호 — 이 앱이 주는 확신 (화면기획서 4.4, 시안 C1). 화면에서 가장 강하게 보인다.
 * 「또 갈래」는 라임 카드, 그 밖의 평가는 같은 크기의 어두운 카드다 (라임은 친구가 검증한 곳에만).
 */
export function FriendSignalCard({ signal }: { signal: FriendSignal }) {
  const again = signal.rating === 'AGAIN';
  const shown = signal.people.slice(0, MAX_AVATARS);
  const unanimous = signal.count === signal.total && signal.total > 1;
  const sub =
    again && unanimous
      ? '친구들이 만장일치로 또 가고 싶대요'
      : signal.rating === 'NOPE'
        ? '친구들은 한 번이면 충분했대요'
        : null;
  return (
    <View style={[styles.card, again ? styles.again : styles.plain]} accessibilityRole="summary">
      <View style={styles.stack} aria-hidden>
        {shown.map((person, i) => (
          <View
            key={person.id}
            style={[
              styles.avatar,
              { borderColor: again ? colors.accent.default : colors.surface.sheet },
              i > 0 && styles.overlap,
            ]}
          >
            <Avatar uri={person.avatarUrl} name={person.name} size={size.signalAvatar} />
          </View>
        ))}
      </View>
      <View style={styles.text}>
        <Text style={[styles.headline, !again && styles.headlinePlain]}>{signal.headline}</Text>
        {sub ? <Text style={[styles.sub, !again && styles.subPlain]}>{sub}</Text> : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3.5],
    padding: spacing[4],
    borderRadius: shape.sheet,
    transform: [{ rotate: '-1.2deg' }],
  },
  again: {
    backgroundColor: colors.accent.default,
  },
  plain: {
    backgroundColor: colors.surface.sheet,
    borderWidth: 1,
    borderColor: colors.border.strong,
  },
  stack: {
    flexDirection: 'row',
  },
  avatar: {
    borderRadius: shape.pill,
    borderWidth: 2.5,
    overflow: 'hidden',
  },
  overlap: {
    marginLeft: -spacing[3],
  },
  text: {
    flex: 1,
    gap: spacing[1],
  },
  headline: {
    ...textStyles.headline,
    color: colors.accent.on,
  },
  headlinePlain: {
    color: colors.text.primary,
  },
  sub: {
    ...textStyles.meta,
    color: colors.accent.on,
    opacity: 0.7,
  },
  subPlain: {
    color: colors.text.secondary,
    opacity: 1,
  },
});

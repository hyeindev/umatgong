import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, layout, shape, spacing, textStyles } from '@/theme';

import type { Candidate } from './candidates';
import { RATING_LABEL } from './ratings';

type Props = {
  candidates: readonly Candidate[];
  loading: boolean;
  /** 비었을 때 보여줄 문구 */
  emptyText: string;
  error: string | null;
  onSelect: (candidate: Candidate) => void;
  /** 목록 맨 아래 (「여기 없어요」) */
  footer?: ReactNode;
};

const formatDistance = (meters: number | null) => {
  if (meters === null) {
    return null;
  }
  return meters < 1000 ? `${meters}m` : `${(meters / 1000).toFixed(1)}km`;
};

/** 장소 후보. 친구가 기록한 곳은 라임 글자로 먼저 알려 준다 (화면기획서 4.5) */
export function CandidateList({ candidates, loading, emptyText, error, onSelect, footer }: Props) {
  return (
    <View style={styles.list}>
      {loading && candidates.length === 0 ? (
        <ActivityIndicator color={colors.accent.default} />
      ) : null}
      {error ? (
        <Text style={styles.empty} accessibilityRole="alert">
          {error}
        </Text>
      ) : null}
      {!loading && !error && candidates.length === 0 ? (
        <Text style={styles.empty}>{emptyText}</Text>
      ) : null}
      {candidates.map((candidate) => (
        <CandidateRow
          key={candidate.place.id}
          candidate={candidate}
          onPress={() => onSelect(candidate)}
        />
      ))}
      {footer}
    </View>
  );
}

function CandidateRow({ candidate, onPress }: { candidate: Candidate; onPress: () => void }) {
  const { place, friendCount, friendBest, mine } = candidate;
  const meta = [place.category, formatDistance(place.distanceMeters)].filter(Boolean).join(' · ');
  const signal =
    friendCount > 0 && friendBest
      ? `친구 ${friendCount}명이 기록 · ${RATING_LABEL[friendBest]}`
      : mine
        ? '내가 전에 간 곳'
        : place.clubId !== null
          ? '우리 클럽이 등록한 곳'
          : null;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={[place.name, signal, meta].filter(Boolean).join(', ')}
      onPress={onPress}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}
    >
      <View style={styles.text}>
        <Text style={styles.name} numberOfLines={1}>
          {place.name}
        </Text>
        {signal ? (
          <Text style={[styles.signal, friendCount > 0 && styles.signalFriend]} numberOfLines={1}>
            {signal}
          </Text>
        ) : null}
        {meta ? (
          <Text style={styles.meta} numberOfLines={1}>
            {meta}
          </Text>
        ) : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: spacing[2],
  },
  empty: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
    paddingVertical: spacing[3],
  },
  row: {
    paddingVertical: spacing[3],
    paddingHorizontal: layout.cardPadding,
    borderRadius: shape.card,
    backgroundColor: colors.surface.raised,
  },
  pressed: {
    opacity: 0.85,
  },
  text: {
    gap: spacing[1],
  },
  name: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
  },
  signal: {
    ...textStyles.meta,
    color: colors.text.secondary,
  },
  // 어두운 바탕 위의 “또 갈래” 검증 문장은 라임 (colors.text.onCream.again의 짝)
  signalFriend: {
    color: colors.accent.default,
  },
  meta: {
    ...textStyles.meta,
    color: colors.text.tertiary,
  },
});

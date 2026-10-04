import { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  PanResponder,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';

import { Avatar } from '@/components/Avatar';
import { StickerCard } from '@/components/StickerCard';
import { colors, shape, size, spacing, textStyles } from '@/theme';

import type { PlaceSummary } from './cardSummary';

// 아바타 묶음에 보이는 최대 인원. 나머지는 +N
const MAX_AVATARS = 3;

type Props = {
  summary: PlaceSummary | null;
  loading: boolean;
  error: string | null;
  /** 현재 위치로부터 거리 (예: "350m"). 위치를 모르면 null */
  distance: string | null;
  /** 좌우로 넘길 수 있는 핀 중 몇 번째인지 (0부터) */
  index: number;
  count: number;
  onPrev: () => void;
  onNext: () => void;
  /** 위로 끌어 올림 / 「자세히 보기」 — 장소 상세 */
  onDetail: () => void;
};

/**
 * 미니 카드 (화면기획서 4.2, 시안 ref-all.html A2). 지도 위에 겹쳐 뜬다 — 화면 전환이 아니다.
 * 좌우로 밀면 근처 다른 핀, 위로 끌면 장소 상세. 화면 낭독기는 좌우 넘기기를 조정 동작(increment/decrement)으로 쓴다.
 */
export function MiniCard({
  summary,
  loading,
  error,
  distance,
  index,
  count,
  onPrev,
  onNext,
  onDetail,
}: Props) {
  const [drag] = useState(() => new Animated.ValueXY());
  // 넘긴 뒤(index가 바뀐 뒤)에만 다시 만들어진다. 끄는 도중에는 바뀌지 않는다
  const responder = useMemo(
    () =>
      PanResponder.create({
        // 살짝 누르는 것은 버튼 몫이다. 어느 정도 끌기 시작해야 카드가 가져간다
        onMoveShouldSetPanResponder: (_, g) => Math.abs(g.dx) > 8 || Math.abs(g.dy) > 8,
        onPanResponderMove: (_, g) => {
          // 아래로는 끌리지 않는다 (위로만 펼친다)
          drag.setValue({ x: g.dx, y: Math.min(0, g.dy) });
        },
        onPanResponderRelease: (_, g) => {
          const [prev, next, detail, i, n] = [onPrev, onNext, onDetail, index, count];
          const horizontal = Math.abs(g.dx) > Math.abs(g.dy);
          if (horizontal && g.dx <= -size.swipeThreshold && i < n - 1) {
            next();
          } else if (horizontal && g.dx >= size.swipeThreshold && i > 0) {
            prev();
          } else if (!horizontal && g.dy <= -size.swipeThreshold) {
            detail();
          }
          Animated.spring(drag, { toValue: { x: 0, y: 0 }, useNativeDriver: false }).start();
        },
        onPanResponderTerminate: () => {
          Animated.spring(drag, { toValue: { x: 0, y: 0 }, useNativeDriver: false }).start();
        },
      }),
    [drag, onPrev, onNext, onDetail, index, count],
  );

  const label = summary
    ? `${summary.name}, ${summary.headline}${distance ? `, ${distance}` : ''}`
    : '가게 정보';

  return (
    <View style={styles.wrap} pointerEvents="box-none">
      <Animated.View
        {...responder.panHandlers}
        accessible
        accessibilityLabel={label}
        accessibilityHint={
          count > 1 ? '좌우로 밀면 근처 다른 가게, 위로 밀면 자세히 보기' : '위로 밀면 자세히 보기'
        }
        accessibilityActions={[
          { name: 'increment', label: '다음 가게' },
          { name: 'decrement', label: '이전 가게' },
          { name: 'activate', label: '자세히 보기' },
        ]}
        onAccessibilityAction={(event) => {
          if (event.nativeEvent.actionName === 'increment' && index < count - 1) {
            onNext();
          } else if (event.nativeEvent.actionName === 'decrement' && index > 0) {
            onPrev();
          } else if (event.nativeEvent.actionName === 'activate') {
            onDetail();
          }
        }}
        style={[
          styles.tilt,
          { transform: [{ translateX: drag.x }, { translateY: drag.y }, { rotate: '-2deg' }] },
        ]}
      >
        <StickerCard
          photoHeight={size.miniCardPhoto}
          photo={summary?.thumbnailUrl ? { uri: summary.thumbnailUrl } : null}
          caption={
            <Caption summary={summary} loading={loading} error={error} distance={distance} />
          }
          sticker={summary ? <RatingSticker summary={summary} /> : undefined}
        />
      </Animated.View>

      <View style={styles.footer} pointerEvents="box-none">
        {count > 1 ? <Pager index={index} count={count} /> : null}
        <Pressable
          accessibilityRole="button"
          onPress={onDetail}
          style={({ pressed }) => [styles.detail, pressed && styles.pressed]}
        >
          <Text style={styles.detailLabel}>자세히 보기</Text>
        </Pressable>
      </View>
    </View>
  );
}

function Caption({
  summary,
  loading,
  error,
  distance,
}: {
  summary: PlaceSummary | null;
  loading: boolean;
  error: string | null;
  distance: string | null;
}) {
  if (!summary) {
    return (
      <View style={[styles.caption, styles.captionCenter]}>
        {loading ? (
          <ActivityIndicator color={colors.text.onCream.tertiary} />
        ) : (
          <Text style={styles.meta} accessibilityRole="alert">
            {error ?? '기록을 불러오지 못했어요.'}
          </Text>
        )}
      </View>
    );
  }
  const shown = summary.people.slice(0, MAX_AVATARS);
  const rest = summary.people.length - shown.length;
  const names = summary.people.map((p) => p.name).join(', ');
  return (
    <View style={styles.caption}>
      <View style={styles.titleRow}>
        <Text style={styles.name} numberOfLines={1}>
          {summary.name}
        </Text>
        {distance ? <Text style={styles.distance}>{distance}</Text> : null}
      </View>
      {summary.club ? (
        <View style={styles.clubRow}>
          <View style={[styles.clubDot, { backgroundColor: colors.club[summary.club.color] }]} />
          <Text style={styles.meta} numberOfLines={1}>
            {summary.club.name}
            {summary.otherClubCount > 0 ? ` 외 ${summary.otherClubCount}개 클럽` : ''}
          </Text>
        </View>
      ) : null}
      <View style={styles.peopleRow}>
        <View style={styles.stack} aria-hidden>
          {shown.map((person, i) => (
            <View key={person.id} style={[styles.stackItem, i > 0 && styles.stackOverlap]}>
              <Avatar uri={person.avatarUrl} name={person.name} size={size.avatar.stack} />
            </View>
          ))}
          {rest > 0 ? (
            <View style={[styles.stackItem, styles.stackOverlap, styles.more]}>
              <Text style={styles.moreLabel}>+{rest}</Text>
            </View>
          ) : null}
        </View>
        <Text style={styles.meta} numberOfLines={1}>
          {[summary.category, names].filter(Boolean).join(' · ')}
        </Text>
      </View>
    </View>
  );
}

/** 대표 평가 스티커. 「또 갈래」만 라임이다 (라임은 친구가 검증한 곳이라는 신호에만) */
function RatingSticker({ summary }: { summary: PlaceSummary }) {
  const again = summary.bestRating === 'AGAIN';
  return (
    <View style={[styles.sticker, again ? styles.stickerAgain : styles.stickerPlain]}>
      {again ? (
        <Svg width={13} height={17} viewBox="0 0 26 34">
          <Path
            d="M13 1.5C6.6 1.5 1.5 6.6 1.5 13c0 8.3 11.5 19.5 11.5 19.5S24.5 21.3 24.5 13C24.5 6.6 19.4 1.5 13 1.5z"
            fill={colors.accent.on}
          />
          <Circle cx={13} cy={13} r={4.5} fill={colors.accent.default} />
        </Svg>
      ) : null}
      <Text style={[styles.stickerLabel, !again && styles.stickerLabelPlain]}>
        {summary.headline}
      </Text>
    </View>
  );
}

function Pager({ index, count }: { index: number; count: number }) {
  return (
    <View style={styles.pager} accessibilityLabel={`근처 가게 ${count}곳 중 ${index + 1}번째`}>
      {Array.from({ length: Math.min(count, 7) }, (_, i) => {
        // 7개가 넘으면 지금 위치를 가운데쯤에 두고 보여 준다
        const start = Math.min(Math.max(0, index - 3), Math.max(0, count - 7));
        const on = start + i === index;
        return <View key={i} style={[styles.dot, on && styles.dotOn]} />;
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: spacing[3.5],
  },
  tilt: {
    marginHorizontal: spacing[6],
  },
  caption: {
    paddingTop: spacing[3.5],
    paddingHorizontal: spacing[1.5],
    paddingBottom: spacing[4],
    gap: spacing[2],
  },
  captionCenter: {
    minHeight: spacing[12] + spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    gap: spacing[2.5],
  },
  name: {
    ...textStyles.headline,
    color: colors.text.onCream.primary,
    flexShrink: 1,
  },
  distance: {
    ...textStyles.meta,
    color: colors.text.onCream.tertiary,
  },
  clubRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
  },
  clubDot: {
    width: size.chipDot,
    height: size.chipDot,
    borderRadius: shape.pill,
  },
  peopleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  meta: {
    ...textStyles.meta,
    color: colors.text.onCream.secondary,
    flexShrink: 1,
  },
  stack: {
    flexDirection: 'row',
  },
  // 크림 카드 위에서 아바타끼리 떨어져 보이게 크림 테두리를 두른다 (시안)
  stackItem: {
    borderRadius: shape.pill,
    borderWidth: 2,
    borderColor: colors.surface.cream,
    overflow: 'hidden',
  },
  stackOverlap: {
    marginLeft: -spacing[2],
  },
  more: {
    width: size.avatar.stack + 4,
    height: size.avatar.stack + 4,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.placeholderAvatar.onCream[1],
  },
  moreLabel: {
    ...textStyles.caption,
    color: colors.text.onCream.primary,
  },
  sticker: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
    paddingVertical: spacing[2],
    paddingHorizontal: spacing[3],
    borderRadius: shape.pill,
    transform: [{ rotate: '6deg' }],
    shadowColor: colors.shadow,
    shadowOpacity: 0.5,
    shadowRadius: spacing[3],
    shadowOffset: { width: 0, height: spacing[1.5] },
    elevation: 8,
  },
  stickerAgain: {
    backgroundColor: colors.accent.default,
  },
  stickerPlain: {
    backgroundColor: colors.surface.raised,
  },
  stickerLabel: {
    ...textStyles.label,
    color: colors.accent.on,
  },
  stickerLabelPlain: {
    color: colors.text.primary,
  },
  footer: {
    gap: spacing[3],
  },
  pager: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: spacing[1.5],
  },
  dot: {
    width: spacing[1.5],
    height: spacing[1],
    borderRadius: shape.pill,
    backgroundColor: colors.border.strong,
  },
  dotOn: {
    width: spacing[4],
    backgroundColor: colors.accent.default,
  },
  detail: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  detailLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  pressed: {
    opacity: 0.85,
  },
});

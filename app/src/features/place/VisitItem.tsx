import { Image, StyleSheet, Text, View } from 'react-native';

import { Avatar } from '@/components/Avatar';
import { colors, shape, size, spacing, textStyles } from '@/theme';
import type { Visit } from '@/types/visit';

import { formatVisitDate } from './detail';
import { RatingChip } from './RatingChip';

/** 방문 기록 한 줄. 메모·사진이 없으면 그 줄을 그리지 않아 빈칸이 생기지 않는다 */
export function VisitItem({ visit }: { visit: Visit }) {
  return (
    <View style={styles.row}>
      <Avatar uri={visit.author.avatarUrl} name={visit.author.name} size={size.avatar.sm} />
      <View style={styles.body}>
        <View style={styles.head}>
          <Text style={styles.name} numberOfLines={1}>
            {visit.mine ? '나' : visit.author.name}
          </Text>
          <Text style={styles.date}>{formatVisitDate(visit.visitedAt)}</Text>
          <View style={styles.spacer} />
          <RatingChip rating={visit.rating} />
        </View>
        {visit.memo ? <Text style={styles.memo}>{visit.memo}</Text> : null}
        {visit.thumbnailUrls.length > 0 ? (
          <View style={styles.thumbs}>
            {visit.thumbnailUrls.map((url) => (
              <Image
                key={url}
                source={{ uri: url }}
                style={styles.thumb}
                accessibilityLabel={`${visit.author.name}님이 찍은 사진`}
              />
            ))}
          </View>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: spacing[3],
  },
  body: {
    flex: 1,
    gap: spacing[2],
  },
  head: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
  },
  name: {
    ...textStyles.sectionLabel,
    color: colors.text.primary,
    flexShrink: 1,
  },
  date: {
    ...textStyles.meta,
    color: colors.text.tertiary,
  },
  spacer: {
    flex: 1,
  },
  memo: {
    ...textStyles.body,
    color: colors.text.secondary,
  },
  thumbs: {
    flexDirection: 'row',
    gap: spacing[1.5],
  },
  thumb: {
    width: size.photoSlot,
    height: size.photoSlot,
    borderRadius: shape.thumbnail,
    backgroundColor: colors.surface.placeholder,
  },
});

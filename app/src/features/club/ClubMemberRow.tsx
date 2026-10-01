import { StyleSheet, Text, View } from 'react-native';

import { Avatar } from '@/components/Avatar';
import { colors, shape, size, spacing, textStyles } from '@/theme';
import type { ClubMember } from '@/types/club';

type Props = {
  member: ClubMember;
  isMe: boolean;
};

// TODO: 멤버별 기록 수(시안 “24곳 기록”)는 멤버 목록 API에 값이 생기면 붙인다. 지금은 지어내지 않고 비워 둔다
export function ClubMemberRow({ member, isMe }: Props) {
  return (
    <View style={styles.row}>
      <Avatar uri={member.avatarUrl} name={member.name} size={size.avatar.md} />
      <View style={styles.text}>
        <View style={styles.nameLine}>
          <Text style={styles.name} numberOfLines={1}>
            {member.name}
          </Text>
          {isMe ? <Tag label="나" /> : null}
          {member.owner ? <Tag label="만든 사람" /> : null}
        </View>
      </View>
    </View>
  );
}

function Tag({ label }: { label: string }) {
  return (
    <View style={styles.tag}>
      <Text style={styles.tagLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
  },
  text: {
    flex: 1,
    gap: spacing[1],
  },
  nameLine: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
  },
  name: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
    flexShrink: 1,
  },
  tag: {
    paddingVertical: spacing[1],
    paddingHorizontal: spacing[2],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  tagLabel: {
    ...textStyles.caption,
    color: colors.text.secondary,
  },
});

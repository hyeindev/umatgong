import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BackButton } from '@/components/BackButton';
import { clubErrorMessage, useMyClubs } from '@/features/club';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';
import type { Club } from '@/types/club';

// 클럽 목록 (화면기획서 4.8). 색·이름·멤버 수.
// TODO: 클럽별 기록 수는 클럽 API에 값이 생기면 붙인다. 지금은 지어내지 않고 비워 둔다
export default function ClubListScreen() {
  const router = useRouter();
  const clubs = useMyClubs();

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.top}>
        <BackButton label="지도" fallback="/" />
        <Text style={styles.title} accessibilityRole="header">
          내 클럽
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.list}>
        {clubs.isPending ? <ActivityIndicator color={colors.accent.default} /> : null}
        {clubs.isError ? (
          <Text style={styles.body} accessibilityRole="alert">
            {clubErrorMessage(clubs.error)}
          </Text>
        ) : null}
        {clubs.data?.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>아직 클럽이 없어요</Text>
            <Text style={styles.body}>
              클럽을 만들고 친구를 초대하면, 친구들이 또 가고 싶다고 한 곳이 내 지도에 떠요.
            </Text>
          </View>
        ) : null}
        {clubs.data?.map((club) => (
          <ClubRow key={club.id} club={club} onPress={() => router.push(`/clubs/${club.id}`)} />
        ))}
      </ScrollView>

      <Pressable
        accessibilityRole="button"
        onPress={() => router.push('/clubs/new')}
        style={({ pressed }) => [styles.create, pressed && styles.pressed]}
      >
        <Text style={styles.createLabel}>새 클럽 만들기</Text>
      </Pressable>
    </SafeAreaView>
  );
}

function ClubRow({ club, onPress }: { club: Club; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${club.name}, 멤버 ${club.memberCount}명${club.full ? ', 정원 꽉 참' : ''}`}
      onPress={onPress}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}
    >
      <View style={[styles.dot, { backgroundColor: colors.club[club.color] }]} />
      <View style={styles.rowText}>
        <Text style={styles.rowName} numberOfLines={1}>
          {club.name}
        </Text>
        <Text style={styles.rowMeta}>
          멤버 {club.memberCount}명{club.owner ? ' · 내가 만든 클럽' : ''}
        </Text>
      </View>
      {club.full ? (
        <View style={styles.fullTag}>
          <Text style={styles.fullTagLabel}>정원 꽉 참</Text>
        </View>
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[6],
  },
  top: {
    paddingTop: spacing[3],
    gap: spacing[5],
  },
  title: {
    ...textStyles.display,
    color: colors.text.primary,
  },
  list: {
    paddingVertical: spacing[6],
    gap: spacing[2.5],
  },
  empty: {
    gap: spacing[2.5],
  },
  emptyTitle: {
    ...textStyles.headline,
    color: colors.text.primary,
  },
  body: {
    ...textStyles.body,
    color: colors.text.secondary,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    padding: layout.cardPadding,
    borderRadius: shape.card,
    backgroundColor: colors.surface.sheet,
  },
  dot: {
    width: size.clubDot,
    height: size.clubDot,
    borderRadius: shape.pill,
  },
  rowText: {
    flex: 1,
    gap: spacing[1.5],
  },
  rowName: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
  },
  rowMeta: {
    ...textStyles.meta,
    color: colors.text.secondary,
  },
  fullTag: {
    paddingVertical: spacing[1],
    paddingHorizontal: spacing[2],
    borderRadius: shape.pill,
    backgroundColor: colors.status.danger,
  },
  fullTagLabel: {
    ...textStyles.caption,
    color: colors.status.onDanger,
  },
  create: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.surface.cream,
    alignItems: 'center',
    justifyContent: 'center',
  },
  createLabel: {
    ...textStyles.button,
    color: colors.text.onCream.primary,
  },
  pressed: {
    opacity: 0.85,
  },
});

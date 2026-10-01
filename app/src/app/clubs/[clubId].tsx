import { useLocalSearchParams, useRouter } from 'expo-router';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BackButton } from '@/components/BackButton';
import {
  ClubCapacity,
  ClubFullNotice,
  ClubInviteActions,
  ClubMemberRow,
  LeaveClub,
  clubErrorMessage,
  useClubMembers,
  useMyClub,
} from '@/features/club';
import { useAuthStore } from '@/stores/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// 클럽 상세 (화면기획서 4.8, 시안 ref-all.html D3).
// 정원 표시 → 초대 → 멤버 → 탈퇴 순서.
// TODO: 클럽 색 변경, 클럽·멤버별 기록 수는 API가 생기면 붙인다. 지금은 자리를 만들지 않는다
export default function ClubDetailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ clubId: string }>();
  const clubId = Number(params.clubId);
  const me = useAuthStore((state) => state.user);
  const { club, isPending, isError, error } = useMyClub(clubId);
  const members = useClubMembers(clubId);

  if (isPending) {
    return (
      <SafeAreaView style={[styles.screen, styles.center]}>
        <ActivityIndicator color={colors.accent.default} />
      </SafeAreaView>
    );
  }

  // 내가 멤버가 아니면 목록에 없다. 없는 클럽과 똑같이 보여준다 (클럽 경계)
  if (!club) {
    return (
      <SafeAreaView style={styles.screen}>
        <View style={styles.top}>
          <BackButton label="클럽" fallback="/clubs" />
        </View>
        <View style={styles.center}>
          <Text style={styles.body} accessibilityRole="alert">
            {isError ? clubErrorMessage(error) : '클럽을 찾을 수 없어요.'}
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.top}>
          <BackButton label="클럽" fallback="/clubs" />
          <View style={styles.heading}>
            <View style={[styles.dot, { backgroundColor: colors.club[club.color] }]} />
            <Text style={styles.title} accessibilityRole="header">
              {club.name}
            </Text>
          </View>
        </View>

        <View style={styles.card}>
          <ClubCapacity memberCount={club.memberCount} maxMembers={club.maxMembers} />
          <ClubInviteActions club={club} />
        </View>
        {club.full ? <ClubFullNotice /> : null}

        <View style={styles.section}>
          <Text style={styles.sectionLabel}>멤버</Text>
          {members.isPending ? <ActivityIndicator color={colors.accent.default} /> : null}
          {members.isError ? (
            <Text style={styles.body} accessibilityRole="alert">
              {clubErrorMessage(members.error)}
            </Text>
          ) : null}
          {members.data?.map((member) => (
            <ClubMemberRow key={member.userId} member={member} isMe={member.userId === me?.id} />
          ))}
        </View>

        <LeaveClub clubId={club.id} onLeft={() => router.replace('/clubs')} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
    paddingHorizontal: layout.screenGutter,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    paddingBottom: spacing[8],
    gap: layout.sectionGap,
  },
  top: {
    paddingTop: spacing[3],
    gap: spacing[5],
  },
  heading: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2.5],
  },
  dot: {
    width: size.clubDot,
    height: size.clubDot,
    borderRadius: shape.pill,
  },
  title: {
    ...textStyles.display,
    color: colors.text.primary,
    flexShrink: 1,
  },
  card: {
    gap: spacing[4],
    padding: spacing[5],
    borderRadius: shape.sheet,
    backgroundColor: colors.surface.sheet,
  },
  section: {
    gap: layout.listRowGap,
  },
  sectionLabel: {
    ...textStyles.sectionLabel,
    color: colors.text.secondary,
  },
  body: {
    ...textStyles.body,
    color: colors.text.secondary,
    textAlign: 'center',
  },
});

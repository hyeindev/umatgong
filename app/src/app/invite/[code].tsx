import { useMutation } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, type ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { KakaoLoginButton, loginErrorMessage, signInWithKakao } from '@/features/auth';
import { clubErrorMessage, useJoinClub } from '@/features/club';
import { isApiError } from '@/lib/api';
import { isInviteCode, pendingInvite } from '@/lib/invite';
import { useAuthStore } from '@/stores/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// 초대 링크로 들어오는 합류 화면. 웹은 /invite/코드 주소로, 앱은 umatgong://invite/코드 딥링크로 열린다
// (둘 다 expo-router가 이 화면으로 보낸다). 로그인 전에도 열려야 하므로 로그인 가드 밖에 있다.
//
// 로그인 전이면 코드를 기기에 맡겨 두고 로그인한다. 웹 로그인은 카카오 페이지를 다녀오며 이 화면을 떠나므로,
// 돌아와서 지도 홈이 맡겨 둔 코드를 꺼내 이 화면으로 다시 보낸다 (src/app/index.tsx).
export default function InviteScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ code: string }>();
  const code = typeof params.code === 'string' ? params.code : '';
  const valid = isInviteCode(code);
  const status = useAuthStore((state) => state.status);
  const join = useJoinClub();
  const signIn = useMutation({
    mutationFn: async () => {
      await pendingInvite.save(code);
      await signInWithKakao();
    },
  });

  // 이 화면에서 바로 로그인을 마친 경우(네이티브) 맡겨 둔 코드는 더 필요 없다
  useEffect(() => {
    if (status === 'signedIn') {
      pendingInvite.take();
    }
  }, [status]);

  if (!valid) {
    return (
      <Layout
        title="초대 링크가 올바르지 않아요"
        body="링크가 잘렸거나 잘못 복사된 것 같아요. 친구에게 링크를 다시 받아 주세요."
      >
        <SecondaryButton label="지도로 가기" onPress={() => router.replace('/')} />
      </Layout>
    );
  }

  if (status !== 'signedIn') {
    return (
      <Layout
        title={'클럽 초대를\n받았어요'}
        body="카카오로 로그인하면 바로 합류할 수 있어요. 친구들이 또 가고 싶다고 한 곳이 내 지도에 떠요."
      >
        {signIn.isError ? (
          <Text style={styles.error} accessibilityRole="alert">
            {loginErrorMessage(signIn.error)}
          </Text>
        ) : null}
        <KakaoLoginButton onPress={() => signIn.mutate()} loading={signIn.isPending} />
      </Layout>
    );
  }

  if (join.isSuccess) {
    const club = join.data;
    return (
      <Layout
        title={`「${club.name}」에\n합류했어요`}
        body="이제 이 클럽 친구들의 기록이 내 지도에 보여요."
      >
        <PrimaryButton label="지도 보기" onPress={() => router.replace('/')} />
        <SecondaryButton label="클럽 보기" onPress={() => router.replace(`/clubs/${club.id}`)} />
      </Layout>
    );
  }

  // 이미 멤버면 실패가 아니다. 클럽 목록으로 안내한다
  const alreadyMember = isApiError(join.error, 'ALREADY_CLUB_MEMBER');
  return (
    <Layout
      title={'초대받은 클럽에\n합류할까요?'}
      body="합류하면 클럽 친구들의 기록이 내 지도에 보이고, 내 기록도 친구들과 나눌 수 있어요."
    >
      {join.isError ? (
        <Text style={styles.error} accessibilityRole="alert">
          {clubErrorMessage(join.error)}
        </Text>
      ) : null}
      {alreadyMember ? (
        <PrimaryButton label="내 클럽 보기" onPress={() => router.replace('/clubs')} />
      ) : (
        <PrimaryButton
          label="합류하기"
          loading={join.isPending}
          onPress={() => join.mutate({ inviteCode: code })}
        />
      )}
      <SecondaryButton label="나중에" onPress={() => router.replace('/')} />
    </Layout>
  );
}

function Layout({ title, body, children }: { title: string; body: string; children: ReactNode }) {
  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.copy}>
        <Text style={styles.title} accessibilityRole="header">
          {title}
        </Text>
        <Text style={styles.body}>{body}</Text>
      </View>
      <View style={styles.actions}>{children}</View>
    </SafeAreaView>
  );
}

function PrimaryButton({
  label,
  onPress,
  loading = false,
}: {
  label: string;
  onPress: () => void;
  loading?: boolean;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ busy: loading, disabled: loading }}
      disabled={loading}
      onPress={onPress}
      style={({ pressed }) => [styles.button, styles.primary, pressed && styles.pressed]}
    >
      {loading ? (
        <ActivityIndicator color={colors.accent.on} />
      ) : (
        <Text style={styles.primaryLabel}>{label}</Text>
      )}
    </Pressable>
  );
}

function SecondaryButton({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.button, styles.secondary, pressed && styles.pressed]}
    >
      <Text style={styles.secondaryLabel}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[8],
  },
  copy: {
    flex: 1,
    justifyContent: 'center',
    gap: spacing[4],
  },
  title: {
    ...textStyles.display,
    color: colors.text.primary,
  },
  body: {
    ...textStyles.body,
    color: colors.text.secondary,
  },
  actions: {
    gap: spacing[2.5],
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.text.primary,
    padding: spacing[3],
    borderRadius: shape.button,
    backgroundColor: colors.surface.sheet,
  },
  button: {
    height: size.button,
    borderRadius: shape.button,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primary: {
    backgroundColor: colors.accent.default,
  },
  primaryLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  secondary: {
    backgroundColor: colors.surface.raised,
  },
  secondaryLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  pressed: {
    opacity: 0.85,
  },
});

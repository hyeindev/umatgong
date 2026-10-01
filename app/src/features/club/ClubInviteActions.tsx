import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { inviteLinks } from '@/lib/invite';
import { sharing, type ShareResult } from '@/lib/share';
import { colors, shape, size, spacing, textStyles } from '@/theme';
import type { Club } from '@/types/club';

import { clubErrorMessage } from './errorMessage';
import { useClubInvite } from './queries';

const RESULT_MESSAGE: Partial<Record<ShareResult, string>> = {
  copied: '초대 링크를 복사했어요. 친구에게 보내 주세요.',
  failed: '복사하지 못했어요. 다시 시도해 주세요.',
};

type Props = { club: Club };

/** 「초대 링크 복사」 + 공유. 정원이 다 찼으면 버튼 대신 막혔다는 것만 보여준다 */
export function ClubInviteActions({ club }: Props) {
  const invite = useClubInvite(club.id, !club.full);
  const [result, setResult] = useState<string | null>(null);

  if (club.full) {
    return (
      <View style={[styles.primary, styles.disabled]} accessibilityState={{ disabled: true }}>
        <Text style={[styles.primaryLabel, styles.disabledLabel]}>초대할 수 없어요</Text>
      </View>
    );
  }

  const link = invite.data ? inviteLinks.linkFor(invite.data.inviteCode) : null;

  const copy = async () => {
    if (!link) {
      return;
    }
    const outcome = await sharing.copy(link);
    setResult(RESULT_MESSAGE[outcome] ?? null);
  };

  const share = async () => {
    if (!link) {
      return;
    }
    const outcome = await sharing.share({
      message: `우맛공 「${club.name}」에 초대해요. 우리끼리 맛집 지도 같이 채워요!`,
      url: link,
    });
    setResult(RESULT_MESSAGE[outcome] ?? null);
  };

  return (
    <View style={styles.wrap}>
      <View style={styles.buttons}>
        <Pressable
          accessibilityRole="button"
          accessibilityState={{ disabled: !link, busy: invite.isPending }}
          disabled={!link}
          onPress={copy}
          style={({ pressed }) => [styles.primary, styles.grow, pressed && styles.pressed]}
        >
          {invite.isPending ? (
            <ActivityIndicator color={colors.accent.on} />
          ) : (
            <Text style={styles.primaryLabel}>초대 링크 복사</Text>
          )}
        </Pressable>
        {sharing.canShare ? (
          <Pressable
            accessibilityRole="button"
            accessibilityState={{ disabled: !link }}
            disabled={!link}
            onPress={share}
            style={({ pressed }) => [styles.secondary, pressed && styles.pressed]}
          >
            <Text style={styles.secondaryLabel}>공유</Text>
          </Pressable>
        ) : null}
      </View>
      {invite.isError ? (
        <Text style={styles.message} accessibilityRole="alert">
          {clubErrorMessage(invite.error)}
        </Text>
      ) : result ? (
        <Text style={styles.message} accessibilityRole="alert">
          {result}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: spacing[2.5],
  },
  buttons: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  grow: {
    flex: 1,
  },
  primary: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  secondary: {
    height: size.button,
    paddingHorizontal: spacing[5],
    borderRadius: shape.button,
    backgroundColor: colors.surface.raised,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  disabled: {
    backgroundColor: colors.surface.raised,
  },
  disabledLabel: {
    color: colors.text.tertiary,
  },
  pressed: {
    opacity: 0.85,
  },
  message: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
});

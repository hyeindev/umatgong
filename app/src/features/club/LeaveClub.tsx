import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

import { clubErrorMessage } from './errorMessage';
import { useLeaveClub } from './queries';

type Props = {
  clubId: number;
  onLeft: () => void;
};

/**
 * 탈퇴. 되돌릴 수 없으므로 한 번 더 확인한다. 확인은 화면 안에서 한다 — 웹(react-native-web)에는
 * Alert 대화상자가 없어서 플랫폼마다 다르게 뜨지 않게 하려는 것이다.
 */
export function LeaveClub({ clubId, onLeft }: Props) {
  const [confirming, setConfirming] = useState(false);
  const leave = useLeaveClub();

  if (!confirming) {
    return (
      <Pressable
        accessibilityRole="button"
        onPress={() => setConfirming(true)}
        style={({ pressed }) => [styles.link, pressed && styles.pressed]}
      >
        <Text style={styles.linkLabel}>클럽에서 나가기</Text>
      </Pressable>
    );
  }

  return (
    <View style={styles.confirm}>
      <Text style={styles.title}>이 클럽에서 나갈까요?</Text>
      <Text style={styles.body}>
        나가면 이 클럽의 기록이 지도에서 사라져요. 내가 남긴 기록은 지워지지 않지만 더 이상 보이지
        않아요. 다시 들어오려면 초대 링크가 필요해요.
      </Text>
      {leave.isError ? (
        <Text style={styles.body} accessibilityRole="alert">
          {clubErrorMessage(leave.error)}
        </Text>
      ) : null}
      <View style={styles.buttons}>
        <Pressable
          accessibilityRole="button"
          disabled={leave.isPending}
          onPress={() => setConfirming(false)}
          style={({ pressed }) => [styles.button, styles.cancel, pressed && styles.pressed]}
        >
          <Text style={styles.cancelLabel}>그대로 있기</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          accessibilityState={{ busy: leave.isPending, disabled: leave.isPending }}
          disabled={leave.isPending}
          onPress={() =>
            // mutate의 onSuccess는 컴포넌트가 사라지면 불리지 않는다. 약속(Promise)으로 받아 확실히 옮긴다
            leave.mutateAsync(clubId).then(onLeft, () => undefined)
          }
          style={({ pressed }) => [styles.button, styles.danger, pressed && styles.pressed]}
        >
          {leave.isPending ? (
            <ActivityIndicator color={colors.status.onDanger} />
          ) : (
            <Text style={styles.dangerLabel}>나가기</Text>
          )}
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  link: {
    alignSelf: 'center',
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[4],
  },
  linkLabel: {
    ...textStyles.label,
    color: colors.text.tertiary,
  },
  confirm: {
    gap: spacing[3],
    padding: layout.cardPadding,
    borderRadius: shape.card,
    backgroundColor: colors.surface.sheet,
    borderWidth: 1,
    borderColor: colors.border.subtle,
  },
  title: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
  },
  body: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
  buttons: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  button: {
    flex: 1,
    height: size.button,
    borderRadius: shape.button,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancel: {
    backgroundColor: colors.surface.raised,
  },
  cancelLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  // 빨강은 삭제 확인에만 (colors.status.danger)
  danger: {
    backgroundColor: colors.status.danger,
  },
  dangerLabel: {
    ...textStyles.button,
    color: colors.status.onDanger,
  },
  pressed: {
    opacity: 0.85,
  },
});

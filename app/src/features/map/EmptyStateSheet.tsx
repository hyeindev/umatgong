import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { photoScanner } from '@/lib/photo';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// 지도 첫 화면의 빈 상태 (docs/design/ref-all.html A3, 화면기획서 4.1 “빈 상태”).
// 아직 기록이 하나도 없을 때 지도 아래에 붙는다.
type Props = {
  /** 사진 없이 직접 기록하기 (웹에서도 되는 수동 경로) */
  onRecord: () => void;
};

export function EmptyStateSheet({ onRecord }: Props) {
  const [notice, setNotice] = useState<string | null>(null);

  // 웹에는 사진첩 API가 없다. 에러가 아니라 앱 설치 안내로 대신한다 (app/AGENTS.md “플랫폼 차이”)
  const onScan = () => {
    setNotice(
      photoScanner.isSupported
        ? '사진첩 스캔은 곧 열려요.'
        : '사진첩에서 찾기는 앱에서 할 수 있어요. 앱을 설치하면 음식 사진으로 첫 핀을 꽂아드려요.',
    );
  };

  return (
    <View style={styles.sheet}>
      <View style={styles.copy}>
        <Text style={styles.title} accessibilityRole="header">
          지도가 아직{'\n'}
          <Text style={styles.titleAccent}>비어 있어요</Text>
        </Text>
        <Text style={styles.body}>
          사진첩의 음식 사진으로 첫 핀을 꽂아드릴게요. 보통 1분이면 끝나요.
        </Text>
      </View>
      {notice ? (
        <Text style={styles.notice} accessibilityRole="alert">
          {notice}
        </Text>
      ) : null}
      <Pressable
        accessibilityRole="button"
        onPress={onScan}
        style={({ pressed }) => [styles.primary, pressed && styles.pressed]}
      >
        <Text style={styles.primaryLabel}>사진첩에서 찾아보기</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        onPress={onRecord}
        style={({ pressed }) => [styles.secondary, pressed && styles.pressed]}
      >
        <Text style={styles.secondaryLabel}>직접 기록하기</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    paddingTop: spacing[6],
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[8],
    gap: spacing[5],
    borderTopLeftRadius: shape.sheet,
    borderTopRightRadius: shape.sheet,
    backgroundColor: colors.background.screen,
    shadowColor: colors.shadow,
    shadowOpacity: 0.6,
    shadowRadius: spacing[10],
    shadowOffset: { width: 0, height: -spacing[5] },
    elevation: 16,
  },
  copy: {
    gap: spacing[2.5],
  },
  title: {
    ...textStyles.display,
    color: colors.text.primary,
  },
  titleAccent: {
    color: colors.accent.default,
  },
  body: {
    ...textStyles.body,
    color: colors.text.secondary,
  },
  notice: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
    padding: spacing[3],
    borderRadius: shape.button,
    backgroundColor: colors.surface.sheet,
  },
  primary: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.85,
  },
  secondary: {
    height: size.button,
    marginTop: -spacing[2],
    borderRadius: shape.button,
    backgroundColor: colors.surface.raised,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  primaryLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
});

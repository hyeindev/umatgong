import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, layout, shape, spacing, textStyles } from '@/theme';

type Props = {
  title: string;
  onClose: () => void;
  children: ReactNode;
};

/** 화면 위에 겹쳐 뜨는 작은 시트. 뒤를 누르면 닫힌다 (길찾기 앱 고르기, 내 평가 고치기) */
export function BottomSheet({ title, onClose, children }: Props) {
  const insets = useSafeAreaInsets();
  return (
    <View style={StyleSheet.absoluteFill}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="닫기"
        onPress={onClose}
        style={[StyleSheet.absoluteFill, styles.scrim]}
      />
      <View
        style={[styles.sheet, { paddingBottom: insets.bottom + spacing[6] }]}
        accessibilityViewIsModal
      >
        <View style={styles.handle} />
        <Text style={styles.title} accessibilityRole="header">
          {title}
        </Text>
        {children}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  scrim: {
    backgroundColor: colors.surface.scrim,
  },
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    paddingTop: spacing[3],
    paddingHorizontal: layout.screenGutter,
    gap: spacing[4],
    borderTopLeftRadius: shape.sheet,
    borderTopRightRadius: shape.sheet,
    backgroundColor: colors.surface.sheet,
  },
  handle: {
    alignSelf: 'center',
    width: spacing[10],
    height: spacing[1],
    borderRadius: shape.pill,
    backgroundColor: colors.border.strong,
  },
  title: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
  },
});

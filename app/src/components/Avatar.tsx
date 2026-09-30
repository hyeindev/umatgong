import { Image, StyleSheet, Text, View } from 'react-native';

import { colors, fontFamily } from '@/theme';

type Props = {
  uri: string | null;
  name: string;
  size: number;
};

/** 프로필 사진. 사진이 없으면 이름 첫 글자를 보여준다 */
export function Avatar({ uri, name, size }: Props) {
  const shape = { width: size, height: size, borderRadius: size / 2 };
  if (uri) {
    return (
      <Image
        source={{ uri }}
        style={[styles.base, shape]}
        accessibilityLabel={`${name} 프로필 사진`}
      />
    );
  }
  return (
    <View style={[styles.base, styles.fallback, shape]} accessibilityLabel={`${name} 프로필`}>
      <Text style={[styles.initial, { fontSize: size * 0.4 }]}>{name.slice(0, 1)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    backgroundColor: colors.surface.placeholder,
  },
  fallback: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  initial: {
    fontFamily: fontFamily.heavy,
    color: colors.text.primary,
  },
});

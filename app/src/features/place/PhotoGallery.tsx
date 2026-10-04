import { Image, ScrollView, StyleSheet, Text, View, useWindowDimensions } from 'react-native';

import { Avatar } from '@/components/Avatar';
import { colors, layout, radius, shape, size, spacing, textStyles } from '@/theme';

import type { GalleryPhoto } from './detail';

// 다음 사진이 살짝 보여야 옆으로 넘길 수 있다는 것이 드러난다
const PHOTO_WIDTH_RATIO = 0.82;

/**
 * 친구들이 찍은 사진, 가로로 넘긴다. 사진마다 왼쪽 아래에 찍은 사람의 아바타.
 * 사진이 하나도 없으면 빈 칸 대신 짧은 안내를 둔다 (사진 없는 기록도 자연스럽게).
 */
export function PhotoGallery({ photos }: { photos: readonly GalleryPhoto[] }) {
  const { width } = useWindowDimensions();
  const photoWidth = Math.round(width * PHOTO_WIDTH_RATIO);

  if (photos.length === 0) {
    return (
      <View style={[styles.empty, { height: size.gallery * 0.55 }]}>
        <Text style={styles.emptyLabel}>아직 사진이 없어요</Text>
      </View>
    );
  }
  if (photos.length === 1 && photos[0]) {
    return (
      <View style={{ height: size.gallery }}>
        <Photo photo={photos[0]} width={width} rounded={false} />
      </View>
    );
  }
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      snapToInterval={photoWidth + spacing[2]}
      decelerationRate="fast"
      contentContainerStyle={styles.row}
      accessibilityLabel={`친구들이 찍은 사진 ${photos.length}장`}
    >
      {photos.map((photo) => (
        <Photo key={`${photo.visitId}-${photo.url}`} photo={photo} width={photoWidth} rounded />
      ))}
    </ScrollView>
  );
}

function Photo({
  photo,
  width,
  rounded,
}: {
  photo: GalleryPhoto;
  width: number;
  rounded: boolean;
}) {
  return (
    <View style={[styles.photo, { width }, rounded && styles.rounded]}>
      <Image
        source={{ uri: photo.url }}
        style={styles.image}
        resizeMode="cover"
        accessibilityLabel={`${photo.author.name}님이 찍은 사진`}
      />
      <View style={styles.byline}>
        <Avatar uri={photo.author.avatarUrl} name={photo.author.name} size={size.avatar.xs} />
        <Text style={styles.bylineLabel} numberOfLines={1}>
          {photo.author.name}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    gap: spacing[2],
    paddingHorizontal: layout.screenGutter,
    height: size.gallery,
  },
  photo: {
    height: size.gallery,
    overflow: 'hidden',
    backgroundColor: colors.surface.placeholder,
  },
  rounded: {
    borderRadius: radius.xl,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  byline: {
    position: 'absolute',
    left: spacing[3],
    bottom: spacing[3],
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
    paddingVertical: spacing[1],
    paddingLeft: spacing[1],
    paddingRight: spacing[2.5],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.onPhoto,
  },
  bylineLabel: {
    ...textStyles.caption,
    color: colors.text.primary,
  },
  empty: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.placeholder,
  },
  emptyLabel: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
});

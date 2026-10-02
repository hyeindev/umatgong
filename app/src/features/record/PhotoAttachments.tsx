import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { photoPicker } from '@/lib/photo';
import { colors, shape, size, spacing, textStyles } from '@/theme';

import { recordErrorMessage } from './errorMessage';
import { uploadThumbnail } from './queries';

/** 한 기록에 붙일 수 있는 사진 수 (docs/api-spec.md: thumbnailUrls 0~3) */
export const MAX_PHOTOS = 3;

type Slot = {
  id: string;
  previewUri: string;
  status: 'uploading' | 'done' | 'failed';
  url?: string;
};

type Props = {
  /** 올리기가 끝난 사진 주소와, 아직 올리는 중인지 */
  onChange: (state: { urls: string[]; uploading: boolean }) => void;
};

/**
 * 사진 (선택). 고르는 즉시 썸네일로 올려 두고, 저장할 때는 받은 주소만 보낸다.
 * 사진 고르기를 지원하지 않는 플랫폼에서는 아무것도 그리지 않는다.
 */
export function PhotoAttachments({ onChange }: Props) {
  const [slots, setSlots] = useState<Slot[]>([]);
  const [error, setError] = useState<string | null>(null);
  const report = useRef(onChange);
  useEffect(() => {
    report.current = onChange;
  }, [onChange]);

  useEffect(() => {
    report.current({
      urls: slots.flatMap((slot) => (slot.status === 'done' && slot.url ? [slot.url] : [])),
      uploading: slots.some((slot) => slot.status === 'uploading'),
    });
  }, [slots]);

  if (!photoPicker.isSupported) {
    return null;
  }

  const update = (id: string, patch: Partial<Slot>) =>
    setSlots((current) => current.map((slot) => (slot.id === id ? { ...slot, ...patch } : slot)));

  const add = async () => {
    setError(null);
    const picked = await photoPicker.pick(MAX_PHOTOS - slots.length);
    if (picked.length === 0) {
      return;
    }
    setSlots((current) => [
      ...current,
      ...picked.map((photo) => ({
        id: photo.id,
        previewUri: photo.previewUri,
        status: 'uploading' as const,
      })),
    ]);
    await Promise.all(
      picked.map(async (photo) => {
        try {
          const uploaded = await uploadThumbnail(photo);
          update(photo.id, { status: 'done', url: uploaded.url });
        } catch (uploadError) {
          update(photo.id, { status: 'failed' });
          setError(recordErrorMessage(uploadError));
        }
      }),
    );
  };

  const remove = (id: string) => setSlots((current) => current.filter((slot) => slot.id !== id));

  return (
    <View style={styles.wrap}>
      <View style={styles.row}>
        {slots.map((slot) => (
          <Pressable
            key={slot.id}
            accessibilityRole="button"
            accessibilityLabel={slot.status === 'failed' ? '올리지 못한 사진 빼기' : '사진 빼기'}
            onPress={() => remove(slot.id)}
            style={styles.slot}
          >
            <Image source={{ uri: slot.previewUri }} style={styles.image} />
            {slot.status === 'uploading' ? (
              <View style={styles.overlay}>
                <ActivityIndicator color={colors.text.primary} />
              </View>
            ) : null}
            {slot.status === 'failed' ? (
              <View style={styles.overlay}>
                <Text style={styles.overlayLabel}>실패</Text>
              </View>
            ) : null}
          </Pressable>
        ))}
        {slots.length < MAX_PHOTOS ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`사진 추가, 최대 ${MAX_PHOTOS}장`}
            onPress={add}
            style={({ pressed }) => [styles.slot, styles.add, pressed && styles.pressed]}
          >
            <Text style={styles.addLabel}>＋ 사진</Text>
          </Pressable>
        ) : null}
      </View>
      {error ? (
        <Text style={styles.error} accessibilityRole="alert">
          {error}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: spacing[2],
  },
  row: {
    flexDirection: 'row',
    gap: spacing[2],
  },
  slot: {
    width: size.photoSlot,
    height: size.photoSlot,
    borderRadius: shape.thumbnail,
    overflow: 'hidden',
    backgroundColor: colors.surface.placeholder,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  overlay: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.scrim,
  },
  overlayLabel: {
    ...textStyles.caption,
    color: colors.text.primary,
  },
  add: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.sheet,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: colors.border.strong,
  },
  addLabel: {
    ...textStyles.label,
    color: colors.text.secondary,
  },
  pressed: {
    opacity: 0.85,
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
});

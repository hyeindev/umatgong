import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { BackButton } from '@/components/BackButton';
import { clubErrorMessage, useCreateClub } from '@/features/club';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// docs/api-spec.md “POST /api/clubs”: 이름 1~30자, 앞뒤 공백은 서버가 지운다
const MAX_NAME_LENGTH = 30;

// 클럽 만들기. 이름만 받는다. 색은 서버가 내 다른 클럽과 겹치지 않게 골라 준다.
export default function CreateClubScreen() {
  const router = useRouter();
  const [name, setName] = useState('');
  const create = useCreateClub();
  const trimmed = name.trim();
  const canSubmit = trimmed.length > 0 && !create.isPending;

  const submit = () => {
    if (!canSubmit) {
      return;
    }
    create.mutate({ name: trimmed }, { onSuccess: (club) => router.replace(`/clubs/${club.id}`) });
  };

  return (
    <SafeAreaView style={styles.screen}>
      <KeyboardAvoidingView behavior="padding" style={styles.flex}>
        <View style={styles.top}>
          <BackButton label="클럽" fallback="/clubs" />
          <Text style={styles.title} accessibilityRole="header">
            클럽 이름을{'\n'}정해 주세요
          </Text>
          <Text style={styles.body}>
            친구들끼리 부르는 이름이면 돼요. 클럽 색은 자동으로 정해져요.
          </Text>
        </View>

        <View style={styles.form}>
          <TextInput
            value={name}
            onChangeText={setName}
            onSubmitEditing={submit}
            placeholder="예: 동네친구들"
            placeholderTextColor={colors.text.tertiary}
            maxLength={MAX_NAME_LENGTH}
            autoFocus
            returnKeyType="done"
            accessibilityLabel="클럽 이름"
            style={styles.input}
          />
          <Text style={styles.counter}>
            {name.length}/{MAX_NAME_LENGTH}
          </Text>
          {create.isError ? (
            <Text style={styles.error} accessibilityRole="alert">
              {clubErrorMessage(create.error)}
            </Text>
          ) : null}
        </View>

        <Pressable
          accessibilityRole="button"
          accessibilityState={{ disabled: !canSubmit, busy: create.isPending }}
          disabled={!canSubmit}
          onPress={submit}
          style={({ pressed }) => [
            styles.submit,
            !canSubmit && styles.submitDisabled,
            pressed && styles.pressed,
          ]}
        >
          {create.isPending ? (
            <ActivityIndicator color={colors.accent.on} />
          ) : (
            <Text style={[styles.submitLabel, !canSubmit && styles.submitLabelDisabled]}>
              클럽 만들기
            </Text>
          )}
        </Pressable>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[6],
  },
  flex: {
    flex: 1,
  },
  top: {
    paddingTop: spacing[3],
    gap: spacing[4],
  },
  title: {
    ...textStyles.title,
    color: colors.text.primary,
    marginTop: spacing[3],
  },
  body: {
    ...textStyles.body,
    color: colors.text.secondary,
  },
  form: {
    flex: 1,
    paddingTop: spacing[8],
    gap: spacing[2],
  },
  input: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
    height: size.button,
    paddingHorizontal: spacing[4],
    borderRadius: shape.button,
    backgroundColor: colors.surface.sheet,
    borderWidth: 1,
    borderColor: colors.border.strong,
  },
  counter: {
    ...textStyles.meta,
    color: colors.text.tertiary,
    alignSelf: 'flex-end',
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
  submit: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitDisabled: {
    backgroundColor: colors.surface.raised,
  },
  submitLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  submitLabelDisabled: {
    color: colors.text.tertiary,
  },
  pressed: {
    opacity: 0.85,
  },
});

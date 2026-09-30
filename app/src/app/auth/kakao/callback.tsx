import { useLocalSearchParams, useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { completeKakaoRedirect, loginErrorMessage } from '@/features/auth';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';

// 카카오 로그인 콜백 (웹). 이 경로는 lib/kakao의 KAKAO_REDIRECT_PATH와 같아야 한다.

type Params = {
  code?: string;
  state?: string;
  error?: string;
  error_description?: string;
};

// 인가 코드는 한 번만 쓸 수 있다. 개발 모드에서 effect가 두 번 돌아도 교환은 한 번만 보낸다.
const exchanged = new Map<string, ReturnType<typeof completeKakaoRedirect>>();

export default function KakaoCallbackScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<Params>();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const key = params.code ?? params.error ?? 'missing';
    let exchange = exchanged.get(key);
    if (!exchange) {
      exchange = completeKakaoRedirect({
        code: params.code,
        state: params.state,
        error: params.error,
        errorDescription: params.error_description,
      });
      exchanged.set(key, exchange);
    }

    let active = true;
    exchange
      .then(() => {
        if (active) {
          router.replace('/');
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setErrorMessage(loginErrorMessage(error));
        }
      });
    return () => {
      active = false;
    };
  }, [params.code, params.state, params.error, params.error_description, router]);

  return (
    <SafeAreaView style={styles.screen}>
      {errorMessage ? (
        <View style={styles.content}>
          <Text style={styles.title}>로그인하지 못했어요</Text>
          <Text style={styles.message} accessibilityRole="alert">
            {errorMessage}
          </Text>
          <Pressable
            accessibilityRole="button"
            onPress={() => router.replace('/login')}
            style={({ pressed }) => [styles.retry, pressed && styles.pressed]}
          >
            <Text style={styles.retryLabel}>다시 로그인</Text>
          </Pressable>
        </View>
      ) : (
        <View style={styles.content}>
          <ActivityIndicator color={colors.accent.default} />
          <Text style={styles.message}>로그인하는 중이에요</Text>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
    paddingHorizontal: layout.screenGutter,
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[4],
  },
  title: {
    ...textStyles.headline,
    color: colors.text.primary,
  },
  message: {
    ...textStyles.body,
    color: colors.text.secondary,
    textAlign: 'center',
  },
  retry: {
    alignSelf: 'stretch',
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.85,
  },
  retryLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
});

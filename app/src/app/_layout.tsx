import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';

import { restoreSession } from '@/features/auth';
import { queryClient } from '@/lib/api';
import { useAppFonts } from '@/lib/fonts';
import { useAuthStore } from '@/stores/auth';
import { colors } from '@/theme';

// 폰트와 저장된 로그인을 확인하기 전에 화면이 한 번 그려졌다가 바뀌지 않도록 스플래시를 잡아 둔다.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useAppFonts();
  const authStatus = useAuthStore((state) => state.status);

  // 폰트 로드에 실패해도 앱은 연다. 글자는 시스템 폰트로 보인다.
  const fontsReady = fontsLoaded || fontError !== null;
  const ready = fontsReady && authStatus !== 'restoring';
  const signedIn = authStatus === 'signedIn';

  useEffect(() => {
    restoreSession();
  }, []);

  useEffect(() => {
    if (ready) {
      SplashScreen.hideAsync();
    }
  }, [ready]);

  if (!ready) {
    return null;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.background.screen },
        }}
      >
        {/* 토큰이 없으면 로그인, 있으면 메인. 막힌 화면으로 가면 열려 있는 첫 화면으로 보낸다 */}
        <Stack.Protected guard={!signedIn}>
          <Stack.Screen name="login" />
        </Stack.Protected>
        <Stack.Protected guard={signedIn}>
          <Stack.Screen name="index" />
          <Stack.Screen name="profile" />
        </Stack.Protected>
        {/* 카카오 콜백은 로그인 전에 열리므로 막지 않는다 */}
        <Stack.Screen name="auth/kakao/callback" />
      </Stack>
    </QueryClientProvider>
  );
}

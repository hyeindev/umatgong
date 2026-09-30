import type { FontSource } from 'expo-font';
import pretendardRegular from 'pretendard/dist/web/static/woff2/Pretendard-Regular.woff2';
import pretendardMedium from 'pretendard/dist/web/static/woff2/Pretendard-Medium.woff2';
import pretendardSemiBold from 'pretendard/dist/web/static/woff2/Pretendard-SemiBold.woff2';
import pretendardBold from 'pretendard/dist/web/static/woff2/Pretendard-Bold.woff2';
import pretendardExtraBold from 'pretendard/dist/web/static/woff2/Pretendard-ExtraBold.woff2';

import { fontFamily } from '@/theme/typography';

// 웹은 woff2를 쓴다. OTF의 절반 크기다.
// 글자 수를 줄인 subset 파일은 더 작지만, 가게 이름에 드문 음절이 나오면 그 글자만
// 다른 폰트로 보이므로 전체 글자가 든 파일을 쓴다.
export const fontSources: Record<string, FontSource> = {
  [fontFamily.regular]: pretendardRegular,
  [fontFamily.medium]: pretendardMedium,
  [fontFamily.semibold]: pretendardSemiBold,
  [fontFamily.bold]: pretendardBold,
  [fontFamily.heavy]: pretendardExtraBold,
};

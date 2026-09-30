import type { FontSource } from 'expo-font';
import pretendardRegular from 'pretendard/dist/public/static/Pretendard-Regular.otf';
import pretendardMedium from 'pretendard/dist/public/static/Pretendard-Medium.otf';
import pretendardSemiBold from 'pretendard/dist/public/static/Pretendard-SemiBold.otf';
import pretendardBold from 'pretendard/dist/public/static/Pretendard-Bold.otf';
import pretendardExtraBold from 'pretendard/dist/public/static/Pretendard-ExtraBold.otf';

import { fontFamily } from '@/theme/typography';

// 네이티브는 OTF를 쓴다 (woff2는 iOS·Android에서 읽지 못한다).
export const fontSources: Record<string, FontSource> = {
  [fontFamily.regular]: pretendardRegular,
  [fontFamily.medium]: pretendardMedium,
  [fontFamily.semibold]: pretendardSemiBold,
  [fontFamily.bold]: pretendardBold,
  [fontFamily.heavy]: pretendardExtraBold,
};

import { useFonts } from 'expo-font';

// 번들러가 플랫폼에 맞춰 fonts.web.ts / fonts.native.ts 중 하나를 고른다.
import { fontSources } from './fonts';

/** Pretendard를 등록한다. 반환값은 [로드 완료 여부, 에러] */
export const useAppFonts = () => useFonts(fontSources);

import type { TextStyle } from 'react-native';

// 폰트는 Pretendard 하나 (디자인 시스템 v2). 폰트 로딩은 별도 작업이라 여기서는 fontFamily를 넣지 않는다.

export const fontSize = {
  '2xs': 11,
  xs: 12.5,
  sm: 13,
  md: 14.5,
  lg: 16,
  xl: 18,
  '2xl': 22,
  '3xl': 26,
  '4xl': 30,
  '5xl': 36,
  '6xl': 52,
  '7xl': 76,
  '8xl': 120,
} as const;

export const fontWeight = {
  regular: '400',
  medium: '500',
  semibold: '600',
  bold: '700',
  heavy: '800',
} as const satisfies Record<string, TextStyle['fontWeight']>;

/** 글자 크기 대비 배수. React Native는 px 값을 받으므로 textStyle()이 곱해서 넣는다 */
export const lineHeight = {
  /** 한 줄짜리 라벨·버튼 */
  none: 1,
  /** 큰 숫자 */
  compact: 0.9,
  /** 제목 */
  tight: 1.15,
  snug: 1.3,
  /** 본문 */
  normal: 1.6,
} as const;

/** em 단위. 글자가 클수록 더 좁힌다 */
export const letterSpacing = {
  normal: 0,
  tight: -0.03,
  tighter: -0.045,
  tightest: -0.05,
  numeric: -0.07,
} as const;

type TextStyleSpec = {
  size: keyof typeof fontSize;
  weight: keyof typeof fontWeight;
  lineHeight: keyof typeof lineHeight;
  letterSpacing?: keyof typeof letterSpacing;
};

const textStyle = (spec: TextStyleSpec): TextStyle => {
  const size = fontSize[spec.size];
  return {
    fontSize: size,
    fontWeight: fontWeight[spec.weight],
    lineHeight: Math.round(size * lineHeight[spec.lineHeight]),
    letterSpacing: size * letterSpacing[spec.letterSpacing ?? 'normal'],
  };
};

export const textStyles = {
  /** 스캔 개수, 연말 결산처럼 화면을 차지하는 숫자 */
  jumbo: textStyle({
    size: '8xl',
    weight: 'heavy',
    lineHeight: 'compact',
    letterSpacing: 'numeric',
  }),
  /** 모은 곳 수 같은 자랑하는 숫자 */
  hero: textStyle({ size: '6xl', weight: 'heavy', lineHeight: 'tight', letterSpacing: 'tightest' }),
  /** 장소 이름, 화면 제목 */
  display: textStyle({
    size: '4xl',
    weight: 'heavy',
    lineHeight: 'tight',
    letterSpacing: 'tighter',
  }),
  /** 시트 제목, 스티커 카드 이름 */
  headline: textStyle({
    size: '2xl',
    weight: 'heavy',
    lineHeight: 'tight',
    letterSpacing: 'tighter',
  }),
  title: textStyle({ size: 'xl', weight: 'bold', lineHeight: 'snug', letterSpacing: 'tight' }),
  /** 목록 행 이름 */
  itemTitle: textStyle({ size: 'lg', weight: 'bold', lineHeight: 'snug', letterSpacing: 'tight' }),
  button: textStyle({ size: 'lg', weight: 'heavy', lineHeight: 'none', letterSpacing: 'tight' }),
  body: textStyle({ size: 'md', weight: 'regular', lineHeight: 'normal' }),
  /** 설명, 안내 문구 */
  bodySmall: textStyle({ size: 'xs', weight: 'regular', lineHeight: 'normal' }),
  /** 섹션 이름 */
  sectionLabel: textStyle({ size: 'sm', weight: 'bold', lineHeight: 'none' }),
  /** 칩, 평가 라벨 */
  label: textStyle({ size: 'xs', weight: 'bold', lineHeight: 'none' }),
  /** 거리·날짜 같은 메타 정보 */
  meta: textStyle({ size: 'xs', weight: 'medium', lineHeight: 'none' }),
  caption: textStyle({ size: '2xs', weight: 'semibold', lineHeight: 'none' }),
} as const;

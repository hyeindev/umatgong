import type { TextStyle } from 'react-native';

// 폰트는 Pretendard 하나 (디자인 시스템 v2). 로딩은 src/lib/fonts가 맡는다.
//
// 굵기는 fontWeight가 아니라 굵기별 family로 고른다. Android는 커스텀 폰트에서
// fontWeight로 굵기 파일을 찾지 못하므로, 파일마다 family 이름을 따로 등록한다.
export const fontFamily = {
  regular: 'Pretendard-Regular',
  medium: 'Pretendard-Medium',
  semibold: 'Pretendard-SemiBold',
  bold: 'Pretendard-Bold',
  heavy: 'Pretendard-ExtraBold',
} as const;

/**
 * 여러 화면에서 반복되는 크기만 둔다.
 * 한 화면에만 나오는 큰 숫자(스캔 개수, 연말 결산 등)는 스케일에 넣지 않고
 * 그 화면에서 fontSize를 직접 지정한다.
 */
export const fontSize = {
  xs: 11,
  sm: 12.5,
  md: 13,
  base: 14.5,
  lg: 16,
  xl: 22,
  '2xl': 26,
  '3xl': 30,
} as const;

/** 글자 크기 대비 배수. React Native는 px 값을 받으므로 textStyle()이 곱해서 넣는다 */
export const lineHeight = {
  /** 한 줄짜리 라벨·버튼 */
  none: 1,
  /** 화면을 차지하는 큰 숫자 */
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
  /** 화면을 차지하는 큰 숫자 */
  numeric: -0.07,
} as const;

type TextStyleSpec = {
  size: keyof typeof fontSize;
  weight: keyof typeof fontFamily;
  lineHeight: keyof typeof lineHeight;
  letterSpacing?: keyof typeof letterSpacing;
};

const textStyle = (spec: TextStyleSpec): TextStyle => {
  const size = fontSize[spec.size];
  return {
    fontFamily: fontFamily[spec.weight],
    fontSize: size,
    lineHeight: Math.round(size * lineHeight[spec.lineHeight]),
    letterSpacing: size * letterSpacing[spec.letterSpacing ?? 'normal'],
  };
};

export const textStyles = {
  /** 장소 이름, 클럽 이름 */
  display: textStyle({
    size: '3xl',
    weight: 'heavy',
    lineHeight: 'tight',
    letterSpacing: 'tightest',
  }),
  /** 화면 제목 (예: 어디서 먹었어요?) */
  title: textStyle({ size: '2xl', weight: 'heavy', lineHeight: 'tight', letterSpacing: 'tighter' }),
  /** 시트 제목, 스티커 카드 이름 */
  headline: textStyle({
    size: 'xl',
    weight: 'heavy',
    lineHeight: 'tight',
    letterSpacing: 'tighter',
  }),
  /** 목록 행 이름 */
  itemTitle: textStyle({ size: 'lg', weight: 'bold', lineHeight: 'snug', letterSpacing: 'tight' }),
  button: textStyle({ size: 'lg', weight: 'heavy', lineHeight: 'none', letterSpacing: 'tight' }),
  body: textStyle({ size: 'base', weight: 'regular', lineHeight: 'normal' }),
  /** 설명, 안내 문구 */
  bodySmall: textStyle({ size: 'sm', weight: 'regular', lineHeight: 'normal' }),
  /** 섹션 이름 */
  sectionLabel: textStyle({ size: 'md', weight: 'bold', lineHeight: 'none' }),
  /** 칩, 평가 라벨 */
  label: textStyle({ size: 'sm', weight: 'bold', lineHeight: 'none' }),
  /** 거리·날짜 같은 메타 정보 */
  meta: textStyle({ size: 'sm', weight: 'medium', lineHeight: 'none' }),
  caption: textStyle({ size: 'xs', weight: 'semibold', lineHeight: 'none' }),
} as const;

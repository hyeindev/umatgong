// 4 단위 스케일. 촘촘한 칩·아이콘 간격을 위해 2px 반 단계를 둔다.
export const spacing = {
  0: 0,
  0.5: 2,
  1: 4,
  1.5: 6,
  2: 8,
  2.5: 10,
  3: 12,
  3.5: 14,
  4: 16,
  5: 20,
  6: 24,
  7: 28,
  8: 32,
  10: 40,
  12: 48,
} as const;

/** 반복되는 배치 값 (디자인 시스템 v2 “간격”) */
export const layout = {
  /** 화면 좌우 여백 */
  screenGutter: spacing[5],
  /** 카드 안쪽 여백 */
  cardPadding: spacing[4],
  /** 섹션 사이 */
  sectionGap: spacing[7],
  /** 목록 행 사이. 구분선 없이 간격으로만 나눈다 */
  listRowGap: spacing[3.5],
} as const;

export const radius = {
  xs: 4,
  sm: 10,
  md: 12,
  lg: 16,
  xl: 18,
  '2xl': 24,
  full: 999,
} as const;

/**
 * 쓰임새별 radius (디자인 시스템 v2 “라운드”: 12 썸네일 · 18 버튼·카드 · 24 시트 · 999 칩).
 * 화면 시안에는 20·22·28이 섞여 있지만 시스템에서 벗어난 값이므로 따르지 않는다.
 */
export const shape = {
  /** 목록·후보 썸네일 */
  thumbnail: radius.md,
  /** 스티커(폴라로이드) 카드 */
  stickerCard: radius.lg,
  button: radius.xl,
  card: radius.xl,
  /** 바텀 시트 위쪽 모서리 */
  sheet: radius['2xl'],
  /** 칩, 스티커 라벨, 배지 */
  pill: radius.full,
} as const;

/** 반복되는 요소 크기 */
export const size = {
  /** 주요 버튼 높이 (디자인 시스템 v2 버튼 52~58) */
  button: 56,
  /** 지도 위 떠 있는 원형·사각 버튼 (현재 위치, 프로필) */
  floatingButton: 48,
  avatar: {
    /** 카드 안에 겹쳐 놓는 작은 아바타 */
    xs: 18,
    sm: 36,
    lg: 72,
  },
} as const;

// 디자인 시스템 v2 (docs/design/design-system.html) 기준.
// 화면 코드는 palette를 직접 쓰지 않고 아래 colors의 의미 토큰만 쓴다.

const palette = {
  ink950: '#121211',
  ink900: '#1d1d1b',
  ink850: '#232321',
  ink800: '#262624',
  ink700: '#31302d',
  ink600: '#3a3833',
  ink500: '#45423d',
  stone200: '#d8d4ce',
  stone300: '#c2bcb3',
  stone350: '#ada69b',
  stone400: '#6d6b64',
  sage800: '#3a5641',
  cream: '#faf9f7',
  lime: '#d4f53c',
  red: '#e5484d',
  kakaoYellow: '#fee500',
  white: '#ffffff',
  black: '#000000',
} as const;

// 어두운 바탕 위의 글자·선은 크림색에 투명도만 달리해서 만든다.
const creamAlpha = (alpha: number) => `rgba(250, 249, 247, ${alpha})`;
const inkAlpha = (alpha: number) => `rgba(29, 29, 27, ${alpha})`;

export const colors = {
  background: {
    /** 화면 바깥, 가장 깊은 바탕 */
    deep: palette.ink950,
    /** 기본 화면 바탕. 사진 하단 그라데이션도 이 색으로 녹인다 */
    screen: palette.ink900,
  },

  surface: {
    /** 바텀 시트, 떠 있는 버튼 */
    sheet: palette.ink800,
    /** 칩, 보조(다크) 버튼 */
    raised: palette.ink700,
    /** 스티커 카드, 선택된 칩, 크림(보조) 버튼 */
    cream: palette.cream,
    /** 사진이 들어오기 전 자리, 기본 아바타 */
    placeholder: palette.ink600,
    /** 지도 위에 떠 있는 반투명 칩 (blur와 함께) */
    floating: 'rgba(49, 48, 45, 0.85)',
    /** 사진 위 메타 정보 칩 (blur와 함께) */
    onPhoto: inkAlpha(0.55),
    /** 카드가 떴을 때 뒤 지도를 가리는 막 */
    scrim: 'rgba(18, 18, 17, 0.45)',
  },

  text: {
    primary: palette.cream,
    secondary: creamAlpha(0.62),
    tertiary: creamAlpha(0.42),
    /** 라임·크림 면 위의 글자는 항상 잉크색 */
    onAccent: palette.ink900,
    onCream: {
      primary: palette.ink900,
      secondary: palette.ink500,
      tertiary: palette.stone400,
      /**
       * 크림 카드 위의 “또 갈래” 검증 문장. 라임 글자는 크림 위에서 읽히지 않으므로
       * 세이지를 어둡게 내린 색을 쓴다. 어두운 바탕 위에서는 accent(라임)를 쓴다.
       */
      again: palette.sage800,
    },
  },

  border: {
    /** 카드 테두리, 구분선 */
    subtle: creamAlpha(0.08),
    /** 점선 빈 칸, 시트 손잡이 */
    strong: creamAlpha(0.18),
  },

  /** 라임은 “또 갈래”와 화면의 주요 행동 1개에만 쓴다. 한 화면에 라임 면은 2개까지 */
  accent: {
    default: palette.lime,
    on: palette.ink900,
  },

  status: {
    /** 정원 초과·삭제 확인에만 */
    danger: palette.red,
    onDanger: palette.white,
  },

  brand: {
    /** 카카오 로그인 버튼에만 */
    kakao: palette.kakaoYellow,
    onKakao: 'rgba(0, 0, 0, 0.85)',
    /** 카카오 말풍선 심볼 */
    kakaoSymbol: palette.black,
  },

  /**
   * 실제 사람이 아닌 자리표시 아바타 (로그인 화면 예시 카드 등). 겹쳐 놓았을 때 구분되도록 3단계.
   * 크림 카드 위에서 쓴다. 클럽 색이 아니다.
   */
  placeholderAvatar: {
    onCream: [palette.stone200, palette.stone300, palette.stone350],
  },

  pin: {
    /** 또 갈래 — 클럽과 무관하게 라임 물방울 */
    again: palette.lime,
    /** 한 번은 — 흐린 점, 기본 숨김. 괜찮아는 club 색을 쓴다 */
    nope: creamAlpha(0.3),
    /** 핀 테두리 — 지도 위에서 핀을 떼어 보이게 한다 */
    outline: palette.ink900,
    /** 내 위치 — 크림색 점 + 옅은 후광 */
    me: palette.cream,
    meHalo: creamAlpha(0.14),
    /**
     * 가까운 핀 묶음(클러스터). 라임 면은 한 화면에 2개까지라 바탕은 어둡게 두고,
     * 「또 갈래」가 들어 있는 묶음만 라임 테두리로 표시한다.
     */
    cluster: palette.ink700,
    clusterLabel: palette.cream,
    clusterAgain: palette.lime,
  },

  /**
   * 클럽 색. 키는 API의 club color 값과 같다.
   * 라임과 겹치지 않게 전부 저채도이고, 빨강 계열은 넣지 않는다 (경고색 status.danger와 충돌).
   */
  club: {
    SAGE: '#8fb098',
    SKY: '#8fb3c9',
    SAND: '#d1b98a',
    LILAC: '#b3a3d1',
  },

  shadow: palette.black,
} as const;

export type ClubColor = keyof typeof colors.club;

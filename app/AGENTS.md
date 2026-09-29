# app/ — 프론트엔드

우맛공 프론트. **하나의 코드베이스로 iOS · Android · 웹을 모두 빌드한다.**
백엔드는 `../api`에 있다.
**루트 `AGENTS.md`의 공통 규칙을 함께 따른다.** 이 문서는 프론트 전용 규칙만 담는다.

기획 문서: `../docs/기획서.md`, `../docs/화면기획서.md`
API 명세: `../docs/api-spec.md` — 백엔드가 원본. 여기서 수정하지 않는다
디자인 시안: `../docs/design/*.html`

---

## 스택

- Expo (React Native) + React Native Web
- TypeScript, expo-router
- TanStack Query (서버 상태) / zustand (클라이언트 상태)
- 카카오맵 — 웹은 JS SDK, 네이티브는 네이티브 SDK

## 명령어

```bash
npx expo start --dev-client    # 네이티브 개발
npx expo start --web           # 웹 개발
npx expo export -p web         # 웹 정적 빌드
eas build -p android           # 앱 빌드
npm run typecheck              # 커밋 전 필수
npm run lint                   # 커밋 전 필수
```

## 디렉터리 구조

```
src/
  app/            expo-router 화면
  components/     재사용 컴포넌트
  features/       도메인별 로직 (map, record, club, place, auth)
  lib/
    api/          API 클라이언트, JWT 인터셉터
    map/          지도 추상화 (플랫폼별)
    storage/      토큰 저장 (플랫폼별)
    photo/        사진첩 접근 (네이티브 전용)
  stores/  theme/  types/
```

---

## 절대 규칙

### 플랫폼 차이 — 이 프로젝트에서 가장 중요

**웹은 사진첩에 접근할 수 없다.** 브라우저에는 사진 라이브러리 API가 없고,
파일 선택 다이얼로그로 사용자가 직접 고르는 것만 가능하다.

| 기능 | 네이티브 | 웹 |
|---|---|---|
| 사진첩 자동 스캔 | 지원 | **미지원** |
| 온보딩 대량 스캔 | 지원 | **미지원** |
| 지도 조회 / 내 근처 맛집 | 지원 | 지원 |
| 기록하기 | 자동 카드 | 수동 업로드 |
| 클럽 초대 링크 열람 | 지원 | 지원 |

**웹은 유입과 열람, 네이티브는 기록.** 웹에 사진첩 기능이 없는 것은 결함이 아니라
설계다. 기획서 C-06(웹 링크로 지도 공유)이 웹의 존재 이유다.

웹에서 네이티브 전용 기능에 접근하면 **에러를 띄우지 말고 앱 설치를 안내**한다.

### 플랫폼 분기는 경계에서만

`Platform.OS` 분기를 화면 코드에 흩뿌리지 않는다.
플랫폼별 파일 확장자로 `lib/` 안에서 흡수하고, 화면은 하나의 인터페이스만 쓴다.

```
src/lib/map/        Map.types.ts / Map.web.tsx / Map.native.tsx / index.ts
src/lib/storage/    token.web.ts / token.native.ts / index.ts
src/lib/photo/      scanner.web.ts(미지원 반환) / scanner.native.ts / index.ts
```

화면 코드는 `import { Map } from '@/lib/map'` 하나만 쓴다.
**화면에서 `Platform.OS`를 보는 코드가 생기면 그 로직은 `lib/`로 내려야 한다는 신호다.**

### API 호출

- 모든 호출은 `lib/api` 클라이언트를 거친다. 컴포넌트에서 직접 fetch 금지
- 서버 상태는 TanStack Query로. `useEffect` + `fetch` 패턴 금지
- **카카오 REST API를 직접 호출하지 않는다.** 장소 검색·경로는 전부 백엔드 경유
  (예외: 웹 지도 렌더링용 JS SDK. JavaScript 키를 쓰며 **도메인 제한 필수**)
- 401 응답 시 리프레시 토큰으로 자동 갱신 후 원 요청 재시도.
  갱신 실패 시에만 로그인 화면으로

### 좌표

- 내부 타입과 API 통신 모두 `{ lat, lng }`로 통일
- 카카오의 x/y 표기는 백엔드 안에만 존재한다. 프론트는 모른다

### 지도 성능

- 지도 이동이 멈춘 뒤 조회 (디바운스 300ms 이상). 이동 중 매 프레임 요청 금지
- 화면에 보이는 영역의 핀만 요청
- 소요시간은 리스트에 실제로 렌더링되는 항목에만 요청 (화면기획서 4.3)

### UI

- 첫 화면은 무조건 지도다. 클럽 선택이나 중간 화면을 넣지 않는다
- 기록하기의 후보 목록과 평가 버튼은 **화면 하단 절반**에 둔다 (한 손 조작)
- 필터는 즉시 반영. 적용 버튼을 만들지 않는다
- 평가 구분은 **형태** — 「또 갈래」 라임 물방울 핀 / 「괜찮아」 클럽 색 원 / 「한 번은」 흐린 점(기본 숨김)
- 클럽 구분은 **색** — 「괜찮아」 핀과 클럽 칩의 색 점. 「또 갈래」 핀은 클럽과 무관하게 라임
- 클럽 색 팔레트에서 빨강 계열 제외 (경고색과 충돌)

### 디자인 시안

`../docs/design/` 아래 HTML 시안을 화면 작업 전에 읽는다.

**시안은 참조이지 소스가 아니다.** React Native에는 HTML/CSS가 없으므로
마크업을 이식하지 말고 아래만 가져온다.

- 색상값, 폰트 크기·굵기·자간
- 간격과 radius
- 배치 순서와 시각적 위계
- 컴포넌트 상태 변화

옮기지 않는 것: div 구조, CSS 선택자, hover, grid, 웹 전용 속성

**색과 간격은 시안에서 직접 복사하지 말고 `src/theme/`의 토큰을 쓴다.**
시안과 화면기획서가 어긋나면 화면기획서가 맞다. 시안은 겉모습, 기획서는 동작이다.

---

## 코드 규칙

- 함수형 컴포넌트만. class 컴포넌트 금지
- `any` 금지. API 응답 타입은 `../docs/api-spec.md` 기준으로 `types/`에 정의
- 스타일은 `StyleSheet.create` 또는 정해진 스타일 유틸
- 주석은 "왜"를 쓴다. "무엇"은 코드가 말한다

## 작업 방식

- 한 번에 한 화면
- 화면을 만들면 **웹과 네이티브 양쪽에서 확인**한다
- 기존 파일 수정 전에 먼저 읽는다
- 불확실하면 추측하지 말고 묻는다
- 커밋 전 `npm run typecheck && npm run lint` 통과

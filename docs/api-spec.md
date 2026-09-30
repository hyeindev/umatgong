# 우맛공 API 명세

> 백엔드(`api/`)가 원본이다. API를 추가·변경하면 같은 커밋에서 이 문서를 갱신한다.
> 프론트(`app/`)는 이 문서를 기준으로 타입을 정의하고, 여기서 수정하지 않는다.

---

## 1. 공통

### 기본

- Base URL: 환경마다 다름 (로컬 `http://localhost:8080`)
- 요청·응답 본문: `application/json; charset=utf-8`
- 날짜·시간: ISO 8601 UTC (`2026-09-30T00:00:00Z`). 표시용 변환은 클라이언트가 한다
- 좌표: 항상 `{ "lat": number, "lng": number }`. 카카오의 x/y 표기는 쓰지 않는다

### 응답 형식

모든 응답은 `ApiResponse`로 감싼다.

성공

```json
{ "success": true, "data": { ... } }
```

`data`가 없는 성공(로그아웃 등)은 `{ "success": true }`.

실패

```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "요청 값이 올바르지 않습니다.",
    "fieldErrors": [ { "field": "kakaoAccessToken", "reason": "공백일 수 없습니다" } ]
  }
}
```

- **분기는 `error.code`로 한다.** `message`는 사용자에게 보여줄 수 있는 문구지만 바뀔 수 있다
- `fieldErrors`는 본문 검증 실패일 때만 온다

### 인증

인증이 필요한 API는 헤더에 액세스 토큰을 넣는다.

```
Authorization: Bearer {accessToken}
```

| 구분 | 형식 | 만료 | 보관 |
|---|---|---|---|
| 액세스 토큰 | JWT (HS256) | 30분 | 메모리 권장 |
| 리프레시 토큰 | 무작위 문자열 (JWT 아님) | 14일, 갱신할 때마다 다시 14일 | 네이티브: SecureStore / 웹: 아래 참고 |

- 토큰은 헤더로 주고받는다. 쿠키를 쓰지 않으므로 서버 CORS는 `credentials`를 허용하지 않는다
- 웹에서 리프레시 토큰을 어디에 둘지는 프론트 결정 사항이다. 쿠키 방식으로 바꾸려면 서버 CORS·CSRF 설정도 함께 바꿔야 한다

### 401 처리 규칙

| `error.code` | 뜻 | 클라이언트 동작 |
|---|---|---|
| `EXPIRED_TOKEN` | 액세스 토큰 만료 | `POST /api/auth/refresh` 후 원 요청 재시도 |
| `INVALID_TOKEN` | 액세스 토큰 위조·형식 오류 | 갱신 시도 1회, 실패하면 로그인 화면 |
| `UNAUTHORIZED` | 토큰 없음 | 로그인 화면 |
| `INVALID_REFRESH_TOKEN` | 리프레시 토큰 무효 | **갱신을 멈추고** 저장된 토큰을 지운 뒤 로그인 화면 |
| `KAKAO_INVALID_TOKEN` | 카카오 토큰 무효 | 카카오 SDK로 다시 로그인 |

> **갱신 요청은 한 번에 하나만 보낸다.** 리프레시 토큰은 한 번 쓰면 폐기된다(회전).
> 같은 리프레시 토큰으로 갱신이 두 번 가면 두 번째는 재사용으로 판단돼 그 기기의 로그인이 끊긴다.
> 여러 요청이 동시에 401을 받으면 갱신은 하나만 보내고 나머지는 그 결과를 기다린다.

### 에러 코드

| code | HTTP | 설명 |
|---|---|---|
| `INVALID_INPUT` | 400 | 요청 값 검증 실패, 읽을 수 없는 본문 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `INVALID_TOKEN` | 401 | 액세스 토큰이 유효하지 않음 |
| `EXPIRED_TOKEN` | 401 | 액세스 토큰 만료 |
| `KAKAO_INVALID_TOKEN` | 401 | 카카오 토큰이 무효이거나 우리 앱에서 발급된 토큰이 아님 |
| `INVALID_REFRESH_TOKEN` | 401 | 리프레시 토큰이 없거나 만료·폐기됨 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 없는 경로·리소스 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| `KAKAO_UNAVAILABLE` | 502 | 카카오 서버 장애·타임아웃. 잠시 후 재시도 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

---

## 2. 인증 (`/api/auth`)

세 API 모두 **액세스 토큰 없이** 호출한다.

### POST /api/auth/kakao — 카카오 로그인

클라이언트가 카카오 SDK로 로그인해 받은 **카카오 액세스 토큰**을 보내면, 서버가 카카오에 검증한 뒤
우리 서비스의 토큰을 발급한다. 처음 로그인한 사용자는 이때 가입된다.

- 사용자 식별은 카카오 회원번호로 한다. 이메일은 받지 않는다
- 카카오 동의항목은 닉네임·프로필 이미지뿐이다. 동의하지 않았으면 이름은 `"이름 없음"`, 사진은 `null`
- 이미 가입한 사용자는 로그인할 때마다 카카오의 닉네임·프로필 이미지로 갱신된다
- 카카오 액세스 토큰은 검증에만 쓰고 저장하지 않는다
- 같은 사용자로 동시에 여러 번 호출해도(버튼 연타, 재시도) 모두 성공하고 사용자는 한 명만 생긴다.
  이때 `newUser`가 `true`인 응답은 하나뿐이다

요청

```json
{ "kakaoAccessToken": "카카오 SDK가 준 액세스 토큰" }
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `kakaoAccessToken` | string | O | 카카오 SDK 로그인 결과의 액세스 토큰 |

응답 `200`

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresIn": 1800,
    "refreshToken": "Qx3...43자",
    "refreshTokenExpiresIn": 1209600,
    "user": {
      "id": 7,
      "name": "지현",
      "avatarUrl": "https://k.kakaocdn.net/..."
    },
    "newUser": true
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `accessToken` | string | 우리 서비스의 액세스 토큰 |
| `accessTokenExpiresIn` | number | 초 |
| `refreshToken` | string | 리프레시 토큰 |
| `refreshTokenExpiresIn` | number | 초 |
| `user.id` | number | 사용자 ID |
| `user.name` | string | 카카오 닉네임. 동의 안 했으면 `"이름 없음"` |
| `user.avatarUrl` | string \| null | 카카오 프로필 이미지. 없거나 카카오 기본 이미지면 `null` |
| `newUser` | boolean | 이번에 가입했으면 `true`. 첫 실행 온보딩(S1)을 띄우는 기준 |

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `kakaoAccessToken`이 비어 있음 |
| 401 | `KAKAO_INVALID_TOKEN` | 카카오 토큰 만료·위조, 또는 다른 앱에서 발급된 토큰 |
| 502 | `KAKAO_UNAVAILABLE` | 카카오 서버 응답 없음 |

### POST /api/auth/refresh — 토큰 갱신

리프레시 토큰으로 새 액세스 토큰과 **새 리프레시 토큰**을 받는다. 보낸 리프레시 토큰은 폐기되므로
응답의 `refreshToken`으로 반드시 교체해 저장한다.

요청

```json
{ "refreshToken": "Qx3..." }
```

응답 `200`

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresIn": 1800,
    "refreshToken": "새 리프레시 토큰",
    "refreshTokenExpiresIn": 1209600
  }
}
```

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `refreshToken`이 비어 있음 |
| 401 | `INVALID_REFRESH_TOKEN` | 없는 토큰, 만료된 토큰, 이미 쓴 토큰 |

> 이미 쓴(폐기된) 리프레시 토큰이 다시 오면 탈취로 보고, 그 로그인에서 이어진 리프레시 토큰을 모두 폐기한다.
> 그 기기는 다시 로그인해야 한다. 다른 기기의 로그인은 영향이 없다.

### POST /api/auth/logout — 로그아웃

이 기기의 로그인만 끝낸다. 액세스 토큰이 만료된 상태에서도 호출할 수 있도록 리프레시 토큰으로 처리한다.
이미 로그아웃된 토큰이나 모르는 토큰을 보내도 성공으로 응답한다.

- 서버는 리프레시 토큰만 폐기한다. 이미 발급된 액세스 토큰은 만료(최대 30분)까지 유효하므로
  클라이언트가 직접 지운다

요청

```json
{ "refreshToken": "Qx3..." }
```

응답 `200`

```json
{ "success": true }
```

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `refreshToken`이 비어 있음 |

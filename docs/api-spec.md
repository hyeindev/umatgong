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
| `KAKAO_INVALID_TOKEN` | 카카오 토큰·인가 코드 무효 | 카카오 SDK로 다시 로그인 |

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
| `KAKAO_INVALID_TOKEN` | 401 | 카카오 토큰이 무효이거나 우리 앱에서 발급된 토큰이 아님. 웹 로그인에서는 인가 코드 만료·재사용, redirectUri 불일치 |
| `KAKAO_REDIRECT_URI_NOT_ALLOWED` | 400 | 웹 로그인의 `redirectUri`가 서버 허용 목록에 없음 |
| `INVALID_REFRESH_TOKEN` | 401 | 리프레시 토큰이 없거나 만료·폐기됨 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 없는 경로·리소스 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| `CLUB_NOT_FOUND` | 404 | 없는 클럽이거나 내가 속하지 않은 클럽 (둘을 구분하지 않는다) |
| `INVALID_INVITE_CODE` | 404 | 없는 초대 코드, 재발급으로 무효가 된 코드 |
| `ALREADY_CLUB_MEMBER` | 409 | 이미 그 클럽의 멤버 |
| `CLUB_FULL` | 409 | 클럽 정원이 다 참. 새 초대·합류만 막힌다 |
| `CLUB_LIMIT_REACHED` | 409 | 한 사람이 속할 수 있는 클럽 수를 넘음 |
| `PLACE_NOT_FOUND` | 404 | 없는 장소, 또는 다른 클럽의 커스텀 장소 (둘을 구분하지 않는다) |
| `VISIT_NOT_FOUND` | 404 | 없는 기록, 또는 내게 보이지 않는 기록 (다른 클럽, 남의 비공개) |
| `PHOTO_INVALID` | 400 | 올릴 수 없는 사진 (그림 아님, 크기·해상도 초과) |
| `PHOTO_STORAGE_UNAVAILABLE` | 503 | 사진 저장소 미설정·장애 |
| `KAKAO_UNAVAILABLE` | 502 | 카카오 서버 장애·타임아웃·호출 한도 초과. 잠시 후 재시도 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

---

## 2. 인증 (`/api/auth`)

네 API 모두 **액세스 토큰 없이** 호출한다.

카카오 로그인은 두 경로다. 어느 경로로 로그인해도 같은 카카오 회원은 같은 사용자이고, 응답도 같다.

| 클라이언트 | API | 보내는 값 |
|---|---|---|
| 네이티브 앱 | `POST /api/auth/kakao` | 카카오 네이티브 SDK가 준 액세스 토큰 |
| 웹 | `POST /api/auth/kakao/code` | 카카오 JS SDK `authorize`가 리다이렉트로 준 인가 코드 |

> 웹은 카카오 JS SDK가 액세스 토큰을 직접 주지 않고, 코드를 토큰으로 바꾸려면 REST API 키가 필요하다.
> REST API 키는 서버에만 두므로 교환은 서버가 한다.

### POST /api/auth/kakao — 카카오 로그인 (네이티브)

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

### POST /api/auth/kakao/code — 카카오 로그인 (웹)

카카오 JS SDK `Kakao.Auth.authorize({ redirectUri })`가 리다이렉트로 돌려준 **인가 코드**를 보내면,
서버가 카카오 인증 서버에서 코드를 카카오 액세스 토큰으로 바꾼 뒤 `POST /api/auth/kakao`와 같은 과정으로
로그인한다. 가입·프로필 갱신·동시 호출 처리도 같다.

- `redirectUri`는 **authorize에 보낸 값과 문자열까지 같아야 한다.** 서버는 받은 값을 그대로 카카오에 넘긴다
- `redirectUri`는 서버 설정(`KAKAO_ALLOWED_REDIRECT_URIS`)의 허용 목록에 있어야 한다. 비교는 완전 일치다
  (끝의 `/`, 대소문자, 쿼리가 달라도 거부). 목록에 없으면 카카오를 부르지 않고 거부한다
- 인가 코드는 한 번만 쓸 수 있다. 같은 코드로 두 번 보내면 두 번째는 `KAKAO_INVALID_TOKEN`이다
- 인가 코드와 카카오 토큰은 저장하지 않는다

요청

```json
{
  "code": "카카오가 리다이렉트로 준 인가 코드",
  "redirectUri": "https://umatgong.app/auth/kakao/callback"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `code` | string | O | 인가 코드. 최대 1024자 |
| `redirectUri` | string | O | authorize에 보낸 redirectUri. 최대 2048자 |

응답 `200`: `POST /api/auth/kakao`와 같다 (`accessToken`, `refreshToken`, `user`, `newUser` …).

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `code`·`redirectUri`가 비어 있거나 너무 김 |
| 400 | `KAKAO_REDIRECT_URI_NOT_ALLOWED` | `redirectUri`가 서버 허용 목록에 없음 |
| 401 | `KAKAO_INVALID_TOKEN` | 인가 코드 만료·재사용, authorize 때와 `redirectUri`가 다름 |
| 500 | `INTERNAL_ERROR` | 카카오가 서버의 REST API 키·Client Secret을 거부 (서버 설정 오류) |
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

---

## 3. 장소 (`/api/places`)

모든 API에 **액세스 토큰이 필요하다.** 로그인하지 않은 요청은 카카오를 호출하지 않고 401을 돌려준다.

카카오 로컬 API 결과를 서버가 대신 조회해 돌려준다. 프론트는 카카오 REST API를 직접 호출하지 않는다.

- 음식점(`FD6`)과 카페(`CE7`)만 다룬다
- 좌표는 `lat`, `lng`로 주고받는다. 카카오의 x/y 표기는 서버 안에서만 쓴다
- 조회한 장소는 서버의 `places`에 저장된다. 같은 가게는 여러 검색에 나와도 같은 `id`다
- **검색 결과는 최대 1시간 캐시된다.** 같은 검색이면 카카오를 다시 부르지 않으므로
  그동안 카카오에서 바뀐 가게 정보는 늦게 반영될 수 있다
- 카카오 한 페이지(검색 종류당 최대 15곳)만 조회한다. 페이지 넘김은 아직 없다
- **클럽 전용 장소**(「여기 없어요」로 직접 등록, 아래 `POST /api/places/custom`)는 그 클럽의 **현재 멤버에게만**
  주변·검색 결과에 섞여 나온다. 캐시하지 않고 요청마다 요청자의 지금 클럽으로 거른다

### 장소 응답 (`Place`)

```json
{
  "id": 12,
  "name": "망원동 김반장",
  "address": "서울 마포구 포은로 1",
  "category": "곱창,막창",
  "coordinate": { "lat": 37.5556, "lng": 126.9106 },
  "distanceMeters": 120,
  "clubId": null
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 우리 서비스의 장소 ID. 기록을 남길 때 이 값을 쓴다 |
| `name` | string | 가게 이름 |
| `address` | string \| null | 도로명 주소. 없으면 지번 주소 |
| `category` | string \| null | 업종의 가장 구체적인 단계. 예: `"곱창,막창"`, `"커피전문점"` |
| `coordinate` | `{ lat, lng }` | 가게 좌표 |
| `distanceMeters` | number \| null | 요청 좌표로부터의 직선거리(미터). 요청에 좌표가 없으면 `null` |
| `clubId` | number \| null | 클럽 전용 장소면 그 클럽 ID, 카카오 장소면 `null`. 클럽 전용 장소에는 **그 클럽으로만** 기록할 수 있다 |

> `distanceMeters`는 직선거리다. "걸어서 7분" 같은 소요시간은 별도의 경로 API로 제공할 예정이다.

### GET /api/places/nearby — 주변 음식점·카페

좌표 반경 안의 음식점과 카페를 합쳐 **가까운 순**으로 돌려준다.
기록하기의 후보 목록, 위치 지정의 주변 후보에 쓴다.

쿼리 파라미터

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `lat` | number | O | 위도 (-90 ~ 90) |
| `lng` | number | O | 경도 (-180 ~ 180) |
| `radius` | number | | 반경(미터). 1 ~ 20000, 기본 500 |

```
GET /api/places/nearby?lat=37.5556&lng=126.9106&radius=500
```

응답 `200`

```json
{ "success": true, "data": [ { "id": 12, "name": "망원동 김반장", ... } ] }
```

결과가 없으면 `data`는 빈 배열이다.

- 좌표는 소수점 넷째 자리(약 11m)까지 같으면 같은 검색으로 보고 캐시를 쓴다
- 내 클럽 전용 장소도 반경 안이면 함께 섞어 가까운 순으로 준다

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `lat`/`lng` 누락·범위 밖, `radius` 범위 밖 |
| 401 | `UNAUTHORIZED` 등 | 액세스 토큰 없음·만료 |
| 502 | `KAKAO_UNAVAILABLE` | 카카오 장애·타임아웃·호출 한도 초과. 잠시 후 재시도 |

### GET /api/places/search — 키워드 검색

가게명 등으로 검색한다. 음식점·카페만 돌려주며 **카카오의 정확도 순서**를 유지한다.

쿼리 파라미터

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `query` | string | O | 검색어. 공백만은 안 됨, 최대 100자 |
| `lat` | number | | 거리 표시 기준 위도. `lng`와 함께 보낸다 |
| `lng` | number | | 거리 표시 기준 경도. `lat`와 함께 보낸다 |

```
GET /api/places/search?query=김반장&lat=37.5556&lng=126.9106
```

응답 `200`: `nearby`와 같은 형식. 좌표를 보내지 않았으면 `distanceMeters`는 `null`.

- 좌표는 결과를 거르거나 정렬하지 않는다. 거리 표시에만 쓴다
- 같은 검색어(앞뒤 공백·대소문자 무시)면 좌표가 달라도 같은 캐시를 쓴다
- 이름에 검색어가 들어간 내 클럽 전용 장소(최대 15곳)를 카카오 결과 **앞에** 준다

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `query` 누락·공백, `lat`/`lng` 중 하나만 보냄, 범위 밖 |
| 401 | `UNAUTHORIZED` 등 | 액세스 토큰 없음·만료 |
| 502 | `KAKAO_UNAVAILABLE` | 카카오 장애·타임아웃·호출 한도 초과 |

### POST /api/places/custom — 가게 직접 등록 (「여기 없어요」)

카카오에서 찾을 수 없는 가게를 **클럽 전용**으로 등록한다 (화면기획서 4.6). 그 클럽 멤버에게만 보인다.
다음에 같은 근처에서 찾으면 주변 후보·검색에 뜨고, 클럽 친구에게도 뜬다.

요청

```json
{ "clubId": 3, "name": "광교 할머니 국수", "address": null, "coordinate": { "lat": 37.2887, "lng": 127.0518 } }
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `clubId` | number | O | 내가 멤버인 클럽 |
| `name` | string | O | 1~100자. 앞뒤 공백은 지운다 |
| `address` | string | | 최대 255자 |
| `coordinate` | `{ lat, lng }` | O | 가게 위치 (지도에서 찍은 곳, 또는 지금 내 위치) |

응답 `201`: `{ "success": true, "data": Place }` (`clubId`가 채워져 있다)

- 같은 클럽에 **이름이 같고(대소문자·앞뒤 공백 무시) 50m 안인** 장소가 이미 있으면 새로 만들지 않고 그 장소를 준다
- 다른 클럽에는 같은 가게가 따로 만들어진다 (클럽 경계)

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 이름이 비었거나 100자 초과, 좌표 누락·범위 밖 |
| 404 | `CLUB_NOT_FOUND` | 내가 멤버가 아닌 클럽 (없는 클럽과 구분하지 않음) |

---

## 4. 클럽 (`/api/clubs`)

모든 API에 **액세스 토큰이 필요하다.**

- **클럽 경계:** 클럽 정보는 그 클럽 멤버에게만 보인다. 멤버가 아니면 없는 클럽과 똑같이 `404 CLUB_NOT_FOUND`다.
  다른 클럽이 있다는 사실도 드러내지 않는다
- **정원:** 요금제별 정원(무료 8명, 유료 30명)이 다 차면 **새 초대와 합류만 막는다.** 기존 멤버는 그대로 쓴다
- **클럽 수:** 한 사람은 정해진 수(무료 1개)까지만 클럽에 속할 수 있다. 넘으면 생성·합류가 `CLUB_LIMIT_REACHED`다
- 정원·클럽 수는 서버 설정값이다(기획서 9장). 응답의 `maxMembers`를 보고 화면에 그린다. 숫자를 프론트에 박지 않는다
- 과금은 아직 켜지 않았다. 클럽 수 제한은 무료 기준을 쓴다

### 클럽 응답 (`Club`)

```json
{
  "id": 3,
  "name": "동네친구들",
  "color": "SAGE",
  "memberCount": 5,
  "maxMembers": 8,
  "full": false,
  "owner": true,
  "visitCount": 68,
  "createdAt": "2026-10-01T00:00:00Z"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | number | 클럽 ID |
| `name` | string | 클럽 이름 (1~30자) |
| `color` | `"SAGE"` \| `"SKY"` \| `"SAND"` \| `"LILAC"` | 클럽 색 토큰 이름. 실제 색값은 프론트 테마(`colors.club`)가 가진다. 빨강 계열은 없다 |
| `memberCount` | number | 지금 멤버 수 |
| `maxMembers` | number | 이 클럽 요금제의 정원 |
| `full` | boolean | 정원이 다 찼는지. `true`면 초대·합류가 막힌다 (“정원 꽉 참” 안내) |
| `owner` | boolean | 요청한 사람이 클럽장인지 |
| `visitCount` | number | 이 클럽의 **공개(`CLUB`) 기록 수** (화면의 “함께 모은 곳”). 비공개 기록은 쓴 사람 본인에게도 세지 않는다 — 멤버 누구에게나 같은 숫자다. 탈퇴한 멤버가 남긴 공개 기록도 센다 (지도에 보이는 것과 같다) |
| `createdAt` | string | ISO 8601 UTC |

**색 자동 배정:** 만드는 사람이 이미 속한 클럽과 겹치지 않는 색을 `SAGE → SKY → SAND → LILAC` 순서로 고른다.
네 색을 다 쓰고 있으면 가장 적게 쓰인 색이다.

### POST /api/clubs — 클럽 만들기

만든 사람이 첫 멤버이자 클럽장이 된다. 초대 코드도 함께 만들어진다 (초대 API로 받는다).

요청

```json
{ "name": "동네친구들" }
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | string | O | 1~30자. 앞뒤 공백은 지운다. 공백만은 안 됨 |

응답 `201`: `{ "success": true, "data": Club }`

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 이름이 비었거나 30자 초과 |
| 409 | `CLUB_LIMIT_REACHED` | 이미 속할 수 있는 만큼 클럽에 속해 있음 |

### GET /api/clubs — 내가 속한 클럽 목록

응답 `200`: `{ "success": true, "data": [Club, ...] }`. 들어간 순서. 없으면 빈 배열.

### POST /api/clubs/{clubId}/invite — 초대 코드

이 클럽의 초대 코드를 돌려준다. 프론트가 코드로 초대 링크를 만들어 공유한다. **클럽 멤버만** 부를 수 있다.

- 초대 코드는 추측할 수 없는 32자 무작위 문자열(`[A-Za-z0-9_-]`)이다. 순번·숫자가 아니다
- 본문 없이 부르면 **지금 코드**를 그대로 준다. 이미 보낸 링크는 계속 통한다
- `{"reissue": true}`면 새 코드를 만들고 **이전 코드는 즉시 무효**가 된다 (링크가 퍼졌을 때). 클럽장만 할 수 있다
- 정원이 다 찼으면 코드를 주지 않는다

요청 (선택)

```json
{ "reissue": true }
```

응답 `200`

```json
{ "success": true, "data": { "inviteCode": "q2Vw...32자", "memberCount": 5, "maxMembers": 8 } }
```

에러

| HTTP | code | 상황 |
|---|---|---|
| 403 | `FORBIDDEN` | 클럽장이 아닌데 `reissue: true` |
| 404 | `CLUB_NOT_FOUND` | 없는 클럽, 또는 내가 멤버가 아닌 클럽 |
| 409 | `CLUB_FULL` | 정원이 다 참 |

### POST /api/clubs/join — 초대 코드로 합류

요청

```json
{ "inviteCode": "q2Vw...32자" }
```

응답 `200`: `{ "success": true, "data": Club }` (합류한 클럽)

검사 순서: 코드 확인 → 이미 멤버인지 → 정원 → 내 클럽 수.
같은 클럽의 마지막 자리에 여러 명이 동시에 들어와도 정원을 넘지 않는다 (한 명만 성공, 나머지는 `CLUB_FULL`).

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 코드가 비었거나 64자 초과 |
| 404 | `INVALID_INVITE_CODE` | 없는 코드, 재발급으로 무효가 된 코드 |
| 409 | `ALREADY_CLUB_MEMBER` | 이미 이 클럽의 멤버 |
| 409 | `CLUB_FULL` | 정원이 다 참 |
| 409 | `CLUB_LIMIT_REACHED` | 이미 속할 수 있는 만큼 클럽에 속해 있음 |

### GET /api/clubs/{clubId}/members — 멤버 목록

**클럽 멤버만** 볼 수 있다. 들어온 순서.

응답 `200`

```json
{
  "success": true,
  "data": [
    { "userId": 7, "name": "지현", "avatarUrl": "https://k.kakaocdn.net/...", "owner": true, "visitCount": 24, "joinedAt": "2026-10-01T00:00:00Z" }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `userId` | number | 사용자 ID |
| `name` | string | 카카오 닉네임 |
| `avatarUrl` | string \| null | 프로필 사진 |
| `owner` | boolean | 클럽장인지 |
| `visitCount` | number | 이 멤버가 이 클럽에 남긴 기록 중 **요청자에게 보이는 것**의 수. 남의 비공개 기록은 세지 않고, 요청자 본인 것은 비공개도 센다. 그래서 보는 사람마다 숫자가 다를 수 있다 |
| `joinedAt` | string | ISO 8601 UTC |

에러: `404 CLUB_NOT_FOUND` (없는 클럽, 또는 내가 멤버가 아닌 클럽)

### PATCH /api/clubs/{clubId} — 클럽 색 바꾸기

**클럽장만** 할 수 있다. 클럽장이 탈퇴해 클럽장이 없는 클럽은 아무도 바꿀 수 없다.

요청

```json
{ "color": "LILAC" }
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `color` | string | O | `SAGE` / `SKY` / `SAND` / `LILAC` |

응답 `200`: `{ "success": true, "data": Club }` (바뀐 클럽)

- 바꾼 색은 모든 멤버의 지도 핀·칩에 반영된다 (지도 핀은 캐시가 지나면 새로 받는다)
- 다른 클럽과 색이 겹쳐도 막지 않는다. 자동 배정만 겹치지 않게 고른다

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | `color`가 없거나 팔레트 밖의 값 |
| 403 | `FORBIDDEN` | 멤버지만 클럽장이 아님 (클럽장이 없는 클럽 포함) |
| 404 | `CLUB_NOT_FOUND` | 없는 클럽, 또는 내가 멤버가 아닌 클럽 |

### DELETE /api/clubs/{clubId}/members/me — 탈퇴

이 클럽에서 나간다. 응답 `200`: `{ "success": true }`

- 클럽장이 나가면 클럽장 자리만 비고 클럽과 남은 멤버는 그대로다. 이후 초대 코드 재발급은 할 수 없지만 지금 코드로 초대는 계속된다
- 마지막 멤버가 나가도 클럽은 지우지 않는다 (그 클럽에 남긴 기록이 클럽을 참조한다)
- 탈퇴하면 클럽 수 제한에서 빠진다
- 탈퇴하면 그 클럽 기록이 더 이상 보이지 않는다. 그 클럽에 남긴 내 기록은 지우지 않고 남지만 조회에서 빠진다. 지우려면 `DELETE /api/visits/{id}` (5장)

에러: `404 CLUB_NOT_FOUND` (없는 클럽, 또는 내가 멤버가 아닌 클럽)

---

## 5. 방문 기록 (`/api/visits`)

모든 API에 **액세스 토큰이 필요하다.**

### 보이는 기록 (권한 규칙)

모든 조회는 아래 규칙 하나로 거른다. 서버가 요청마다 요청자의 **지금** 클럽 목록을 DB에서 다시 읽어 적용한다.

| 기록 | 보이는 사람 |
|---|---|
| 클럽 기록, `CLUB` 공개 | 그 클럽의 **현재** 멤버 |
| 클럽 기록, `PRIVATE` | 작성자 본인 (그 클럽의 현재 멤버일 때) |
| 클럽 없이 남긴 기록 (항상 `PRIVATE`) | 작성자 본인 |

- **속하지 않은 클럽의 기록은 어떤 API로도 나오지 않는다.** 탈퇴하면 다음 요청부터 그 클럽 기록이 안 보인다.
  탈퇴한 클럽에 남긴 **내 기록도** 조회에서 빠진다 (지우는 것은 된다, 아래 DELETE)
- 보이지 않는 기록은 “없는 기록”과 응답이 같다 (`404 VISIT_NOT_FOUND`). 있다는 사실도 드러내지 않는다
- 수정·삭제는 작성자 본인만. 남의 기록이 내게 보이면 `403 FORBIDDEN`, 안 보이면 `404 VISIT_NOT_FOUND`
- 평가는 `AGAIN`(또 갈래) / `OKAY`(괜찮아) / `NOPE`(한 번은) 세 가지뿐이다

### 기록 응답 (`Visit`)

```json
{
  "id": 41,
  "place": {
    "id": 12, "name": "망원동 김반장", "address": "서울 마포구 포은로 1", "category": "곱창,막창",
    "coordinate": { "lat": 37.5563, "lng": 126.9236 }
  },
  "club": { "id": 3, "name": "동네친구들", "color": "SAGE" },
  "author": { "id": 7, "name": "지현", "avatarUrl": null },
  "rating": "AGAIN",
  "memo": "막창 최고",
  "visibility": "CLUB",
  "visitedAt": "2026-09-30T12:00:00Z",
  "createdAt": "2026-09-30T12:05:00Z",
  "thumbnailUrl": "https://cdn.example/t1.jpg",
  "thumbnailUrls": ["https://cdn.example/t1.jpg"],
  "mine": false,
  "distanceMeters": null
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `club` | object \| null | 클럽 없이 남긴 기록이면 `null` |
| `memo` | string \| null | 한 줄 메모 (최대 200자) |
| `visibility` | `"CLUB"` \| `"PRIVATE"` | |
| `thumbnailUrl` | string \| null | 대표(첫) 썸네일. 원본 사진은 서버에 없다 |
| `thumbnailUrls` | string[] | 썸네일 전체 (올린 순서, 최대 3장). 없으면 빈 배열 |
| `mine` | boolean | 내가 쓴 기록인지 (수정·삭제 버튼 노출 기준) |
| `distanceMeters` | number \| null | 주변 조회(`/nearby`)에서만 기준 좌표로부터의 직선거리. 그 밖에는 `null` |

### POST /api/visits — 기록 남기기

요청

```json
{
  "placeId": 12,
  "clubId": 3,
  "rating": "AGAIN",
  "memo": "막창 최고",
  "visitedAt": "2026-09-30T12:00:00Z",
  "visibility": "CLUB",
  "thumbnailUrls": ["https://<storage>/storage/v1/object/public/thumbnails/thumbs/7/2f1c....jpg"]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `placeId` | number | O | 장소 ID (`/api/places/nearby`·`/search` 결과) |
| `clubId` | number | | 기록을 남길 클럽. **내가 멤버인 클럽만.** 없으면 나만 보는 기록 |
| `rating` | string | O | `AGAIN` / `OKAY` / `NOPE` |
| `memo` | string | | 최대 200자. 앞뒤 공백을 지우고, 비면 `null` |
| `visitedAt` | string | O | 방문 시각 (ISO 8601). 하루 넘게 미래면 거부 |
| `visibility` | string | | 없으면 `clubId`가 있을 때 `CLUB`, 없을 때 `PRIVATE`. `CLUB`이면 `clubId` 필수 |
| `thumbnailUrls` | string[] | | 0~3개. **`POST /api/photos/thumbnails`로 내가 올린 주소만** 된다 (남이 올린 사진·바깥 주소는 400). 올린 순서대로 붙는다 |

응답 `201`: `{ "success": true, "data": Visit }`

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INVALID_INPUT` | 필수 값 누락, 평가 값 오류, 메모 200자 초과, 내가 올리지 않은 썸네일·4장 이상, 미래 시각, `CLUB`인데 `clubId` 없음 |
| 404 | `CLUB_NOT_FOUND` | 내가 멤버가 아닌 클럽 (없는 클럽과 구분하지 않음) |
| 404 | `PLACE_NOT_FOUND` | 없는 장소, 또는 다른 클럽의 커스텀 장소 |

### GET /api/visits/map — 지도 영역의 핀

지도에 보이는 영역 안의 기록을 핀으로 준다. **지도 이동이 멈춘 뒤 호출한다** (디바운스 300ms 이상).

쿼리 파라미터

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `swLat`, `swLng` | number | O | 영역 남서쪽 끝 |
| `neLat`, `neLng` | number | O | 영역 북동쪽 끝. `sw`보다 북동쪽이어야 한다 |
| `clubIds` | number[] | | 이 클럽들만 (`clubIds=3,5`). 내가 속하지 않은 클럽 ID는 아무것도 돌려주지 않는다 |

```
GET /api/visits/map?swLat=37.40&swLng=126.75&neLat=37.70&neLng=127.20
```

응답 `200`

```json
{
  "success": true,
  "data": [
    { "id": 41, "placeId": 12, "coordinate": { "lat": 37.5563, "lng": 126.9236 },
      "clubColor": "SAGE", "rating": "AGAIN", "thumbnailUrl": "https://cdn.example/t1.jpg" }
  ]
}
```

- 핀은 가볍게 둔다. 메모·작성자 같은 상세는 장소별 기록(`/api/places/{placeId}/visits`)으로 받는다
- 기록 하나가 핀 하나다. 같은 장소의 기록은 `placeId`로 묶어 그린다
- `clubColor`는 클럽 없이 남긴 기록이면 `null`
- 최근 방문순 최대 500개. 넓은 영역(전국)의 클러스터 집계는 아직 없다
- 「한 번은」(`NOPE`)도 내려준다. 기본 숨김은 화면에서 처리한다

### GET /api/visits/nearby — 주변 기록

기준 좌표 반경 안의 기록을 **가까운 순**으로 준다 (「내 근처 맛집」).

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `lat`, `lng` | number | O | 기준 좌표 |
| `radius` | number | | 미터. 1 ~ 20000, 기본 1000 |
| `rating` | string[] | | 이 평가만 (`rating=AGAIN` 또는 `rating=AGAIN,OKAY`) |

응답 `200`: `{ "success": true, "data": [Visit, ...] }` (최대 100개, `distanceMeters` 포함)

### GET /api/places/{placeId}/visits — 장소의 기록

장소 상세 화면용. 그 장소에 남은, **내게 보이는** 기록만 최근 방문순으로 준다 (최대 100개).
보이는 기록이 없으면 빈 배열이다.

에러: `404 PLACE_NOT_FOUND` (없는 장소, 또는 다른 클럽의 커스텀 장소)

### GET /api/visits/me — 내 기록

내 지도용. 내가 쓴 기록 중 지금 보이는 것만 최근 방문순으로 준다. 커서 페이지네이션.

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `cursor` | string | | 이전 응답의 `nextCursor`. 첫 페이지면 빼고 보낸다 |
| `size` | number | | 1 ~ 100, 기본 30 |

응답 `200`

```json
{ "success": true, "data": { "items": [Visit, ...], "nextCursor": "MjAyNi0wOS0..." } }
```

- `nextCursor`가 `null`이면 마지막 페이지다. 커서는 해석하지 말고 그대로 돌려준다
- 페이지 사이에 기록이 추가·삭제돼도 건너뛰거나 겹치지 않는다
- 잘못된 커서는 `400 INVALID_INPUT`

### PATCH /api/visits/{visitId} — 내 기록 고치기

평가와 메모만 고친다. 보낸 필드만 바뀐다.

```json
{ "rating": "OKAY", "memo": "" }
```

| 필드 | 설명 |
|---|---|
| `rating` | 바꿀 평가 |
| `memo` | 바꿀 메모 (최대 200자). `""`이면 메모를 지운다. 필드를 빼면 그대로 |

응답 `200`: `{ "success": true, "data": Visit }`

에러

| HTTP | code | 상황 |
|---|---|---|
| 403 | `FORBIDDEN` | 내게 보이는 남의 기록 |
| 404 | `VISIT_NOT_FOUND` | 없는 기록, 내게 보이지 않는 기록 (탈퇴한 클럽의 내 기록 포함) |

### DELETE /api/visits/{visitId} — 내 기록 지우기

응답 `200`: `{ "success": true }`. 썸네일도 함께 지운다 (사진 저장소의 파일까지. 저장소 삭제가 실패해도 기록 삭제는 되돌리지 않는다).

- 탈퇴한 클럽에 남긴 **내 기록도 지울 수 있다** (기획서 C-04 “내 기록 삭제”)

에러: `403 FORBIDDEN` (내게 보이는 남의 기록) / `404 VISIT_NOT_FOUND` (없거나 보이지 않는 기록)

---

## 6. 스크랩 (가고 싶은 곳)

기획서 E-08. 장소 상세의 「가고싶다」. **나만 보는 목록**이다. 클럽과 나누지 않는다.

- 카카오 장소는 누구나 스크랩할 수 있다
- 클럽 전용 장소(`clubId`가 있는 장소)는 그 클럽의 **지금 멤버**만 다룰 수 있다. 아니면 없는 장소와 같은 `404 PLACE_NOT_FOUND`
- 클럽을 나가면 그 클럽 전용 장소의 스크랩은 목록·상태에서 빠진다 (지우지는 않는다. 다시 들어오면 보인다)

### GET /api/places/{placeId}/scrap — 스크랩했는지

응답 `200`: `{ "success": true, "data": { "placeId": 12, "scrapped": true } }`

### PUT /api/places/{placeId}/scrap — 스크랩하기

여러 번 불러도 결과가 같다 (이미 스크랩했으면 그대로). 응답 `200`: `{ "placeId": 12, "scrapped": true }`

### DELETE /api/places/{placeId}/scrap — 스크랩 풀기

스크랩하지 않은 장소여도 성공한다. 응답 `200`: `{ "placeId": 12, "scrapped": false }`

에러 (세 API 공통): `404 PLACE_NOT_FOUND` (없는 장소, 또는 다른 클럽의 전용 장소)

### GET /api/scraps — 내 스크랩 목록

최근 스크랩순, 최대 200개.

```json
{
  "success": true,
  "data": [
    { "place": { "id": 12, "name": "망원동 김반장", "address": "...", "category": "곱창,막창",
                 "coordinate": { "lat": 37.5563, "lng": 126.9236 }, "distanceMeters": null, "clubId": null },
      "scrappedAt": "2026-10-04T09:00:00Z" }
  ]
}
```

---

## 7. 사진 (`/api/photos`)

### POST /api/photos/thumbnails — 썸네일 올리기

기록에 붙일 사진을 올린다. **원본은 받지 않는다** (기획서 “사진 원본을 서버에 저장하지 않는다”).
프론트가 긴 변 800px 이하의 썸네일로 줄여서 보낸다. 받은 주소를 `POST /api/visits`의 `thumbnailUrls`에 넣는다.

요청: `multipart/form-data`, 파트 이름 `file` (JPEG 또는 PNG)

| 제한 | 값 |
|---|---|
| 크기 | 512KB 이하 (`PHOTO_MAX_BYTES`) |
| 긴 변 | 800px 이하 (`PHOTO_MAX_DIMENSION`). 넘으면 원본으로 보고 거부한다 |

응답 `201`

```json
{ "success": true, "data": { "url": "https://<storage>/storage/v1/object/public/thumbnails/thumbs/7/2f1c....jpg", "width": 640, "height": 480 } }
```

- 서버가 그림을 한 번 풀어 **JPEG로 다시 저장한다.** 촬영 위치(GPS) 같은 EXIF 정보는 남지 않는다
- 저장 경로는 `thumbs/{내 사용자 ID}/{무작위}.jpg`. 이 경로의 주소만 내 기록에 붙일 수 있다
- 주소는 추측할 수 없지만 **주소를 아는 사람은 누구나 열 수 있다** (공개 버킷). 기록을 지우면 파일도 지운다
- 기록에 붙이지 않은 사진은 지금은 저장소에 남는다 (정리 작업은 아직 없다)

에러

| HTTP | code | 상황 |
|---|---|---|
| 400 | `PHOTO_INVALID` | 그림이 아님, 512KB 초과, 긴 변 800px 초과, 파일 없음 |
| 503 | `PHOTO_STORAGE_UNAVAILABLE` | 사진 저장소가 설정되지 않았거나 응답하지 않음. 사진 없이 기록하도록 안내한다 |

---

## 8. 개발 전용 (`/api/dev`)

> ⚠️ **개발·테스트 환경 전용이다. 운영에서는 켜지 않는다.** 앱(`app/`)은 이 API를 호출하지 않는다.
> 기본으로 꺼져 있고, 꺼져 있으면 경로 자체가 없다.

### POST /api/dev/seed — 시드 데이터 만들기

로그인한 사용자에게 **수원 광교 일대 실제 음식점**과 방문 기록이 담긴 클럽을 하나 만들어 준다.
지도·목록 화면을 실제에 가까운 데이터로 확인하기 위한 것이다.

**켜는 조건 (둘 다 필요)**

| 조건 | 설명 |
|---|---|
| 환경변수 `APP_DEV_SEED_ENABLED=true` | 기본 `false`. 꺼져 있으면 API가 등록되지 않는다 |
| 헤더 `X-Dev-Seed-Token` | 서버 환경변수 `APP_DEV_SEED_TOKEN`과 같은 값. 서버 값은 32자 이상이어야 하며, 짧거나 비어 있으면 모든 요청을 거부한다 |

- 둘 중 하나라도 맞지 않으면 **없는 경로와 똑같은 `404 NOT_FOUND`** 를 돌려준다. 403이면 API가 있다는 사실이 드러나기 때문이다.
  같은 이유로 POST가 아닌 메서드도 405가 아니라 404다
- 액세스 토큰도 필요하다 (다른 API와 같다. 없으면 `401`, 없는 경로와 같은 응답)
- 브라우저용이 아니다 (CORS 허용 헤더에 `X-Dev-Seed-Token`이 없다). curl 등으로 부른다

```bash
curl -X POST https://<api>/api/dev/seed \
  -H "Authorization: Bearer <액세스 토큰>" \
  -H "X-Dev-Seed-Token: <APP_DEV_SEED_TOKEN 값>"
```

요청 본문: 없음

**만드는 데이터**

| 항목 | 내용 |
|---|---|
| 클럽 | 이름 `광교 맛집 (시드)`, 내가 클럽장이자 유일한 멤버. 무료 플랜의 클럽 수 제한을 적용하지 않는다 (이미 다른 클럽이 있어도 하나 더 생긴다) |
| 장소 | 광교중앙역·상현역 반경 1km를 카카오 음식점(FD6) 검색 → 최대 20곳을 `places`에 저장 (카카오 호출 2회) |
| 기록 | 장소마다 하나, `CLUB` 공개, 사진 없음 |
| 평가 | 또 갈래 60% / 괜찮아 30% / 한 번은 10% (20곳이면 12 / 6 / 2) |
| 방문 시각 | 최근 6개월(180일)에 고르게 퍼진다. 같은 날 두 번 가지 않는다. 한국 시간 점심(11:30~13:30) 또는 저녁(18:00~20:30) |
| 메모 | 절반만. 나머지는 `null` |

같은 사용자면 평가·시각·메모 배치가 매번 같다.

**멱등:** 여러 번 불러도 중복이 쌓이지 않는다.

- 내 시드 클럽에 기록이 하나라도 있으면 아무것도 만들지 않고(카카오도 부르지 않고) 그대로 돌려준다 (`created: false`)
- 시드 클럽의 기록을 전부 지웠으면 같은 클럽에 다시 채운다. 기록은 (시드 클럽, 장소)마다 하나다
- 동시에 여러 번 불러도 한 벌만 생긴다
- 시드 클럽을 탈퇴하면 다음 호출 때 새 시드 클럽이 생긴다

응답 `200`

```json
{
  "success": true,
  "data": { "clubId": 9, "clubName": "광교 맛집 (시드)", "placeCount": 20, "visitCount": 20, "created": true }
}
```

| 필드 | 설명 |
|---|---|
| `created` | 이번 호출로 무언가 새로 만들었으면 `true`, 이미 있던 시드를 돌려줬으면 `false` |
| `placeCount` | 시드 클럽에 내 기록이 있는 장소 수 |
| `visitCount` | 시드 클럽에 남긴 내 기록 수 |

에러

| HTTP | code | 상황 |
|---|---|---|
| 404 | `NOT_FOUND` | 플래그가 꺼져 있음, 토큰 헤더가 없거나 틀림, POST가 아님 (없는 경로와 같은 응답) |
| 502 | `KAKAO_UNAVAILABLE` | 카카오 장애, 또는 광교 일대 음식점을 하나도 찾지 못함 |

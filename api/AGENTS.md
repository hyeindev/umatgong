# api/ — 백엔드

우맛공 백엔드. 프론트엔드는 `../app`에 있다.
루트 `AGENTS.md`의 공통 규칙을 함께 따른다. 이 문서는 백엔드 전용 규칙만 담는다.

기획 문서: `../docs/기획서.md`, `../docs/화면기획서.md`
API 명세: `../docs/api-spec.md` — **이 프로젝트가 원본이다. 변경 시 같은 커밋에서 갱신**

---

## 스택

- Java 21, Spring Boot 3.x, Gradle
- Spring Security + JWT
- Spring Data JPA + QueryDSL
- PostgreSQL + PostGIS
- Flyway

## 명령어

```bash
./gradlew build
./gradlew bootRun
./gradlew test
docker compose up -d      # 로컬 DB (postgis)
```

## 패키지 구조

```
com.umatgong
  global/
    config/        SecurityConfig, CorsConfig, JpaConfig
    security/      JwtProvider, JwtAuthenticationFilter, CustomUserDetails
    error/         GlobalExceptionHandler, ErrorCode, BusinessException
    kakao/         KakaoLocalClient — 카카오 REST API 호출 전담
    response/      ApiResponse<T>
  domain/
    auth/  user/  club/  place/  visit/
    각 도메인: controller / service / repository / entity / dto
```

---

## 절대 규칙

### 권한 — 가장 중요

**모든 조회는 클럽 경계를 넘지 않는다.** 이 서비스의 신뢰 기반이다.

- 요청자가 속하지 않은 클럽의 `visits`는 어떤 경로로도 반환하지 않는다
- 서비스 레이어에서 클럽 멤버십을 검증한다. 컨트롤러에만 의존하지 않는다
- `visibility=PRIVATE`인 기록은 작성자 본인에게만 반환한다
- **이 규칙은 테스트를 반드시 작성한다.** 다른 클럽 사용자의 조회가 빈 결과를 반환하는지 검증할 것

### 카카오 API

- REST API 키는 이 서버에만 존재한다. 프론트로 내려보내지 않는다
- 카카오 호출은 `KakaoLocalClient`를 통해서만. 다른 곳에서 직접 호출 금지
- 좌표 변환(`x`=경도, `y`=위도)은 이 클래스 안에서만 일어난다. 밖으로 새어나가지 않게 한다
- 장소 조회 전 `external_id`로 `places`를 먼저 찾고, 있으면 카카오를 호출하지 않는다
- 경로 API는 프론트가 요청한 좌표에만 호출한다. 서버가 알아서 전체를 계산하지 않는다
- 경로 결과는 좌표 쌍 기준 캐싱 (TTL 1시간), 한 요청의 도착지 상한 10개
- 카카오 호출마다 로그를 남겨 호출 횟수를 추적 가능하게 한다

### 좌표와 PostGIS

- 도메인 객체는 `Coordinate(double lat, double lng)`로 통일
- DB는 `geography(Point, 4326)`, GIST 인덱스
- **저장 시 `ST_MakePoint(lng, lat)` — 경도가 먼저다.** 여기 틀리면 전부 뒤집힌다

### 인증

- 카카오 로그인으로 가입·인증하고 우리 서버가 자체 JWT를 발급한다
- 액세스 30분 / 리프레시 14일. 리프레시는 DB 저장 + 회전(rotation)
- 웹과 네이티브가 같은 API를 쓴다. 토큰 저장 방식만 클라이언트에서 다르다
- **CORS 필수** — 웹 프론트가 별도 도메인이다. credentials 허용 여부를 토큰 저장 방식(쿠키 vs 헤더)에 맞춰 설정

### 비밀키

- JWT 시크릿, DB 접속 정보, 카카오 키는 환경변수로만 읽는다
- `application.yml`은 커밋한다. 단 실제 값은 넣지 않고 `${환경변수}` 자리만 둔다 (`ApplicationConfigTest`가 검사)
- 로컬 개발값은 `application-local.yml`에 둔다. 이 파일은 커밋하지 않는다

---

## API 설계

- REST. 응답은 `ApiResponse<T>`로 감싼다
- 에러는 `GlobalExceptionHandler`에서 일괄 처리. 컨트롤러에서 try-catch 하지 않는다
- 에러 코드는 `ErrorCode` enum. 프론트가 코드로 분기할 수 있게 한다
- 페이지네이션은 커서 기반 (지도 데이터는 offset이 부적합)
- 날짜·시간은 ISO 8601 UTC로 주고받는다. 표시 변환은 클라이언트 책임

## 코드 규칙

- 엔티티에 `@Setter` 금지. 의미 있는 메서드로 상태를 바꾼다
- 엔티티를 컨트롤러 밖으로 내보내지 않는다. 반드시 DTO 변환
- `@Transactional`은 서비스 레이어에만. 조회는 `readOnly = true`
- N+1을 만들지 않는다. fetch join 또는 `@EntityGraph`
- 테스트는 서비스 레이어 중심. 컨트롤러는 슬라이스 테스트

## 작업 방식

- 한 번에 한 도메인
- 기존 파일 수정 전에 먼저 읽는다
- 불확실하면 추측하지 말고 묻는다
- API를 추가·변경하면 `../docs/api-spec.md`를 같은 커밋에서 갱신한다
- 커밋 전 `./gradlew build` 통과

# 로그인 화면 사진

로그인 화면 스티커 카드에 들어가는 장식용 이미지다. 파일은 이 폴더에 아래 이름으로 둔다.
파일이 없으면 그 카드는 회색 자리로 보인다 (빌드·실행 에러 없음).

| 파일 | 위치 (docs/design/ref-login.html) | 내용 | 출처 |
|---|---|---|---|
| `photo-1.jpg` | 왼쪽 뒤 카드 (`login-photo-1`) | 일식 — 초밥 | 이 프로젝트용으로 직접 그린 일러스트 (`source/sushi.svg`) |
| `photo-2.jpg` | 오른쪽 뒤 카드 (`login-photo-2`) | 한식 — 돌솥비빔밥 | 이 프로젝트용으로 직접 그린 일러스트 (`source/bibimbap.svg`) |
| `photo-3.jpg` | 앞 카드 (`login-photo-3`) | 양식 — 토마토 스파게티 | 이 프로젝트용으로 직접 그린 일러스트 (`source/pasta.svg`) |

- 사진이 아니라 SVG로 그린 일러스트를 800×900 JPG(품질 78)로 내보낸 것이다. 외부 저작물이 아니므로 출처 표시 의무가 없다
- 실제 음식 사진으로 바꾸려면 같은 이름으로 덮어쓰면 된다. 확장자는 `.jpg` `.jpeg` `.png` `.webp` 중 하나
- 카드 폭이 200px 안팎이므로 긴 변 800px, 장당 150KB 이하면 충분하다. 웹 첫 로딩에 그대로 받아진다
- `source/*.svg`는 앱 번들에 들어가지 않는다 (loginPhotos.ts는 photo-1~3만 읽는다)

시안(ref-login.html)의 참고 사진은 Unsplash 사진이다: Dmitry Pavlovsky, Derek Duran, Ben Lei.
앱에는 쓰지 않는다.

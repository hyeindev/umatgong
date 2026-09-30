import type { ImageSourcePropType } from 'react-native';

// 로그인 화면 스티커 카드 사진. app/assets/login/photo-1~3 (CREDITS.md 참고).
//
// 파일을 직접 require하면 파일이 없을 때 번들이 실패한다. require.context는 폴더에 실제로 있는
// 파일만 모으므로, 사진이 없으면 그 자리는 null이 되고 카드는 회색 자리로 보인다.
const photos = require.context(
  '../../../assets/login',
  false,
  /^\.\/photo-[1-3]\.(jpe?g|png|webp)$/,
);

const photoAt = (index: number): ImageSourcePropType | null => {
  const key = photos.keys().find((name) => name.startsWith(`./photo-${index}.`));
  return key ? (photos(key) as ImageSourcePropType) : null;
};

/** 시안의 login-photo-1 (왼쪽 뒤), 2 (오른쪽 뒤), 3 (앞) 순서 */
export const loginPhotos = [photoAt(1), photoAt(2), photoAt(3)] as const;

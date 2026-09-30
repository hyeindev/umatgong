// EXPO_PUBLIC_ 값은 빌드할 때 코드에 박힌다. process.env.X 형태 그대로 써야 치환된다.
const rawApiUrl = process.env.EXPO_PUBLIC_API_URL;

/** 백엔드 주소. 끝의 / 는 뗀다 */
export const API_URL = rawApiUrl ? rawApiUrl.replace(/\/+$/, '') : undefined;

// 번들러가 플랫폼에 맞춰 token.web.ts / token.native.ts 중 하나를 고른다.
export { tokenStorage } from './token';
export type { Tokens, TokenStorage } from './token.types';

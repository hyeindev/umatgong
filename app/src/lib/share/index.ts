// 번들러가 플랫폼에 맞춰 share.web.ts / share.native.ts 중 하나를 고른다.
export { sharing } from './share';
export type { ShareContent, ShareResult, Sharing } from './share.types';

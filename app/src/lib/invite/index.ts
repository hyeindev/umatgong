// 번들러가 플랫폼에 맞춰 invite.web.ts / invite.native.ts 중 하나를 고른다.
// 초대 링크를 여는 쪽은 플랫폼 차이가 없다: 웹 주소(/invite/코드)도, 앱 딥링크(umatgong://invite/코드)도
// expo-router가 같은 화면(src/app/invite/[code].tsx)으로 연다.
export { inviteLinks, pendingInvite } from './invite';
export { isInviteCode } from './pending';
export type { InviteLinks, PendingInviteStore } from './invite.types';

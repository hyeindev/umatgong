export type InviteLinks = {
  /** 친구에게 보낼 초대 링크 */
  linkFor: (inviteCode: string) => string;
};

/**
 * 로그인 전에 연 초대 링크의 코드. 로그인(웹은 카카오 페이지를 다녀온다)을 거치는 동안 잃지 않도록 기기에 둔다.
 * take는 꺼내면서 지운다. 한 번 합류 화면으로 이어 주면 끝이다.
 */
export type PendingInviteStore = {
  save: (inviteCode: string) => Promise<void>;
  take: () => Promise<string | null>;
};

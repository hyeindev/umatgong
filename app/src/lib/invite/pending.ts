// 대기 중인 초대 코드의 저장 형식. 웹·네이티브가 같이 쓴다 (저장소만 다르다).

// 로그인을 미루다 한참 뒤에 로그인했을 때 엉뚱하게 합류 화면이 뜨지 않게 한다
const PENDING_TTL_MS = 60 * 60_000;

// 서버의 초대 코드 형식 ([A-Za-z0-9_-], 최대 64자). 링크에서 이상한 값이 들어오면 저장하지 않는다
const INVITE_CODE = /^[A-Za-z0-9_-]{1,64}$/;

export const isInviteCode = (value: string) => INVITE_CODE.test(value);

export const serializePending = (inviteCode: string, now: number) =>
  JSON.stringify({ inviteCode, savedAt: now });

export const parsePending = (raw: string | null, now: number): string | null => {
  if (!raw) {
    return null;
  }
  try {
    const value: unknown = JSON.parse(raw);
    if (typeof value !== 'object' || value === null) {
      return null;
    }
    const { inviteCode, savedAt } = value as { inviteCode?: unknown; savedAt?: unknown };
    if (
      typeof inviteCode !== 'string' ||
      typeof savedAt !== 'number' ||
      !isInviteCode(inviteCode)
    ) {
      return null;
    }
    return now - savedAt <= PENDING_TTL_MS ? inviteCode : null;
  } catch {
    return null;
  }
};

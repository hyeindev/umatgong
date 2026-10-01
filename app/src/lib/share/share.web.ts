import type { Sharing } from './share.types';

// 웹 공유 API는 모바일 브라우저 대부분과 일부 데스크톱에만 있다. 없으면 복사만 보여준다
const webShare = typeof navigator !== 'undefined' && typeof navigator.share === 'function';

// 클립보드 API가 없거나(http 주소, 오래된 브라우저) 거부됐을 때 쓰는 예전 방식
const copyWithSelection = (text: string) => {
  const field = document.createElement('textarea');
  field.value = text;
  field.setAttribute('readonly', '');
  Object.assign(field.style, { position: 'fixed', opacity: '0', pointerEvents: 'none' });
  document.body.appendChild(field);
  field.select();
  try {
    return document.execCommand('copy');
  } catch {
    return false;
  } finally {
    field.remove();
  }
};

const copy = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    return 'copied' as const;
  } catch {
    return copyWithSelection(text) ? ('copied' as const) : ('failed' as const);
  }
};

export const sharing: Sharing = {
  copy,
  canShare: webShare,
  share: async ({ message, url }) => {
    if (!webShare) {
      return copy(url);
    }
    try {
      await navigator.share({ text: message, url });
      return 'shared';
    } catch (error) {
      return error instanceof DOMException && error.name === 'AbortError' ? 'dismissed' : 'failed';
    }
  },
};

/** copied: 클립보드에 넣음 / shared: 공유 시트로 보냄 / dismissed: 사용자가 공유를 닫음 / failed: 둘 다 못 함 */
export type ShareResult = 'copied' | 'shared' | 'dismissed' | 'failed';

export type ShareContent = {
  /** 공유 시트에 같이 들어가는 한 줄 */
  message: string;
  url: string;
};

export type Sharing = {
  copy: (text: string) => Promise<ShareResult>;
  /** 이 기기에서 공유 시트를 열 수 있는지. 없으면 화면은 공유 버튼을 숨긴다 */
  canShare: boolean;
  share: (content: ShareContent) => Promise<ShareResult>;
};

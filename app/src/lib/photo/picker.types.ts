/** 고른 사진 한 장. 이미 썸네일로 줄어 있다 (원본은 올리지 않는다) */
export type PickedPhoto = {
  id: string;
  /** 화면 미리보기용 주소 */
  previewUri: string;
  /** POST /api/photos/thumbnails에 그대로 보내는 본문 (파트 이름 file) */
  toFormData: () => FormData;
};

export type PhotoPicker = {
  /** false면 화면은 사진 칸을 숨긴다 */
  isSupported: boolean;
  /**
   * 사진을 고르게 하고 썸네일로 줄여 돌려준다. 사용자가 닫으면 빈 배열.
   * @param max 최대 몇 장까지 고를지
   */
  pick: (max: number) => Promise<PickedPhoto[]>;
};

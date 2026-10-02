import type { PhotoPicker } from './picker.types';

// TODO: 네이티브는 사진첩 자동 스캔(scanner)과 함께 expo-image-picker로 붙인다. 지금은 사진 칸을 숨긴다
export const photoPicker: PhotoPicker = {
  isSupported: false,
  pick: async () => [],
};

import type { PhotoPicker, PickedPhoto } from './picker.types';
import { fitWithin, JPEG_QUALITIES, THUMBNAIL_MAX_BYTES, THUMBNAIL_MAX_DIMENSION } from './resize';

// 웹은 사진첩에 접근할 수 없다. 사용자가 파일 선택 창에서 직접 고른 파일만 쓴다 (app/AGENTS.md “플랫폼 차이”)

const chooseFiles = () =>
  new Promise<File[]>((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/jpeg,image/png,image/webp,image/heic';
    input.multiple = true;
    input.addEventListener('change', () => resolve(Array.from(input.files ?? [])));
    input.addEventListener('cancel', () => resolve([]));
    input.click();
  });

const loadImage = (file: File) =>
  new Promise<HTMLImageElement>((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(url);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('사진을 읽을 수 없어요.'));
    };
    image.src = url;
  });

const toBlob = (canvas: HTMLCanvasElement, quality: number) =>
  new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality));

// 캔버스에 다시 그리면 EXIF(촬영 위치 등)도 떨어진다. 서버도 한 번 더 다시 저장한다
const toThumbnail = async (file: File): Promise<Blob> => {
  const image = await loadImage(file);
  const { width, height } = fitWithin(
    image.naturalWidth,
    image.naturalHeight,
    THUMBNAIL_MAX_DIMENSION,
  );
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext('2d');
  if (!context) {
    throw new Error('사진을 줄일 수 없어요.');
  }
  context.drawImage(image, 0, 0, width, height);
  for (const quality of JPEG_QUALITIES) {
    const blob = await toBlob(canvas, quality);
    if (blob && blob.size <= THUMBNAIL_MAX_BYTES) {
      return blob;
    }
  }
  throw new Error('사진이 너무 커요.');
};

let sequence = 0;

export const photoPicker: PhotoPicker = {
  isSupported: true,
  pick: async (max) => {
    const files = (await chooseFiles()).slice(0, Math.max(0, max));
    const picked: PickedPhoto[] = [];
    for (const file of files) {
      try {
        const blob = await toThumbnail(file);
        picked.push({
          id: `web-${Date.now()}-${sequence++}`,
          previewUri: URL.createObjectURL(blob),
          toFormData: () => {
            const form = new FormData();
            form.append('file', blob, 'thumbnail.jpg');
            return form;
          },
        });
      } catch {
        // 읽을 수 없는 파일(HEIC 미지원 브라우저 등)은 건너뛴다
      }
    }
    return picked;
  },
};

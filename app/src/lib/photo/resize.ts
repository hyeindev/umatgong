// 썸네일 크기 계산. 서버 제한(docs/api-spec.md 6장): 긴 변 800px, 512KB 이하

export const THUMBNAIL_MAX_DIMENSION = 800;
export const THUMBNAIL_MAX_BYTES = 512 * 1024;

/** 비율을 지키며 긴 변이 max 이하가 되는 크기. 이미 작으면 그대로 (키우지 않는다) */
export const fitWithin = (width: number, height: number, max: number) => {
  const longSide = Math.max(width, height);
  if (longSide <= max) {
    return { width, height };
  }
  const scale = max / longSide;
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
};

/** JPEG 품질을 이 순서로 낮춰 보며 용량 제한 안에 들어오는 첫 값을 쓴다 */
export const JPEG_QUALITIES = [0.82, 0.7, 0.55, 0.4] as const;

import type { MapBounds } from '@/lib/map';

/**
 * 첫 화면에 꽉 채우는 영역 — 본토와 제주 (docs/화면기획서.md 4.1, docs/design/ref-main.html).
 * 지도는 축소 단계가 두 배씩 뛰어서, 울릉도·독도까지 넣으면 한 단계 멀어져 북한·중국·일본이 크게 보인다.
 * 그래서 첫 화면은 본토 기준으로 잡고, 울릉도·독도는 옆으로 밀어서 보게 한다. 이보다 멀리 축소되지 않는다.
 */
export const SOUTH_KOREA_VIEW: MapBounds = {
  // 제주 남쪽 끝 ~ 휴전선, 태안 ~ 포항 호미곶. 폭 390px 휴대폰에서 한 단계 가깝게 꽉 찬다
  sw: { lat: 33.2, lng: 126.08 },
  ne: { lat: 38.35, lng: 129.6 },
};

/**
 * 남한 전체 (제주·울릉도·독도 포함). 지도를 이 밖으로 옮기지 못하고, 전국 기록 수를 셀 때도 이 영역을 쓴다.
 */
export const SOUTH_KOREA_BOUNDS: MapBounds = {
  sw: { lat: 33.1, lng: 124.6 },
  ne: { lat: 38.7, lng: 131.9 },
};

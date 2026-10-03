// 지도 위 표식의 DOM. 카카오맵 CustomOverlay는 HTML 요소를 받는다.
import { colors, fontFamily, fontSize } from '@/theme';

import type { MapMarker } from './Map.types';

/**
 * 지도 타일은 invert + hue-rotate(180°)로 어둡게 만든다 (Map.web.tsx). 같은 필터를 한 번 더 걸면 원래 색으로
 * 돌아오므로, 지도 위에 그리는 요소에는 이 필터를 걸어 테마 색 그대로 보이게 한다.
 */
export const DARK_TILES = 'invert(1) hue-rotate(180deg)';

const DROPLET_PATH =
  'M13 1.5C6.6 1.5 1.5 6.6 1.5 13c0 8.3 11.5 19.5 11.5 19.5S24.5 21.3 24.5 13C24.5 6.6 19.4 1.5 13 1.5z';
const SVG_NS = 'http://www.w3.org/2000/svg';

// 화면에서 보이는 크기(px). 시안 ref-main.html의 핀·클러스터 크기를 따른다
const DROPLET = { width: 24, height: 31 };
const CIRCLE = 14;
const DOT = 8;
const clusterSize = (count: number) => (count >= 40 ? 52 : count >= 15 ? 44 : count >= 6 ? 38 : 32);

/** CustomOverlay 기준점. 물방울은 아래 끝, 나머지는 가운데가 좌표다 */
export const anchorOf = (marker: MapMarker) =>
  marker.kind === 'pin' && marker.shape === 'droplet' ? { x: 0.5, y: 1 } : { x: 0.5, y: 0.5 };

/** 겹쳤을 때 위에 오는 순서. 묶음 > 또 갈래 > 괜찮아 > 한 번은. 내 위치(10)가 가장 위다 */
export const zIndexOf = (marker: MapMarker) => {
  if (marker.kind === 'cluster') {
    return 4;
  }
  if (marker.selected) {
    return 5;
  }
  return marker.shape === 'droplet' ? 3 : marker.shape === 'circle' ? 2 : 1;
};

const droplet = (color: string) => {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('viewBox', '0 0 26 34');
  svg.setAttribute('width', String(DROPLET.width));
  svg.setAttribute('height', String(DROPLET.height));
  svg.style.display = 'block';
  const path = document.createElementNS(SVG_NS, 'path');
  path.setAttribute('d', DROPLET_PATH);
  path.setAttribute('fill', color);
  path.setAttribute('stroke', colors.pin.outline);
  path.setAttribute('stroke-width', '1.5');
  const hole = document.createElementNS(SVG_NS, 'circle');
  hole.setAttribute('cx', '13');
  hole.setAttribute('cy', '13');
  hole.setAttribute('r', '4.5');
  hole.setAttribute('fill', colors.pin.outline);
  svg.append(path, hole);
  return svg;
};

const round = (diameter: number, color: string, border: number) => {
  const el = document.createElement('div');
  Object.assign(el.style, {
    width: `${diameter}px`,
    height: `${diameter}px`,
    boxSizing: 'border-box',
    borderRadius: '50%',
    background: color,
    border: `${border}px solid ${colors.pin.outline}`,
  });
  return el;
};

const cluster = (count: number, highlighted: boolean) => {
  const diameter = clusterSize(count);
  const el = round(diameter, colors.pin.cluster, 2);
  Object.assign(el.style, {
    borderColor: highlighted ? colors.pin.clusterAgain : colors.pin.outline,
    color: colors.pin.clusterLabel,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: fontFamily.bold,
    fontSize: `${count >= 100 ? fontSize.sm : fontSize.md}px`,
    lineHeight: '1',
  });
  el.textContent = String(count);
  return el;
};

/** 표식 하나의 DOM. 누르면 onPress를 부른다 (키보드 Enter·Space 포함) */
export const markerElement = (marker: MapMarker, onPress: () => void) => {
  const visual =
    marker.kind === 'cluster'
      ? cluster(marker.count, marker.highlighted)
      : marker.shape === 'droplet'
        ? droplet(marker.color)
        : round(
            marker.shape === 'circle' ? CIRCLE : DOT,
            marker.color,
            marker.shape === 'circle' ? 2 : 1,
          );

  const button = document.createElement('div');
  button.setAttribute('role', 'button');
  button.setAttribute('tabindex', '0');
  button.setAttribute('aria-label', marker.label);
  Object.assign(button.style, {
    cursor: 'pointer',
    filter: DARK_TILES,
    // 선택된 핀은 크게. 물방울은 아래 끝이 좌표라 그 점을 기준으로 키운다
    ...(marker.kind === 'pin' && marker.selected
      ? {
          transform: 'scale(1.45)',
          transformOrigin: marker.shape === 'droplet' ? '50% 100%' : '50% 50%',
        }
      : {}),
  });
  if (marker.kind === 'pin' && marker.selected) {
    button.setAttribute('aria-current', 'true');
  }
  button.appendChild(visual);
  button.addEventListener('click', (event) => {
    event.stopPropagation();
    onPress();
  });
  button.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onPress();
    }
  });
  return button;
};

export const myLocationElement = () => {
  const dot = document.createElement('div');
  Object.assign(dot.style, {
    width: '16px',
    height: '16px',
    borderRadius: '50%',
    background: colors.pin.me,
    boxShadow: `0 0 0 8px ${colors.pin.meHalo}`,
    filter: DARK_TILES,
  });
  dot.setAttribute('aria-label', '내 위치');
  return dot;
};

/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test)
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { directionsLinks } from './links.ts';

const dest = { name: '망원동 김반장', coordinate: { lat: 37.5563, lng: 126.9236 } };

describe('directionsLinks', () => {
  it('카카오맵: 앱은 ep=위도,경도, 웹은 /link/to/이름,위도,경도', () => {
    const links = directionsLinks('kakao', dest);
    assert.equal(links.app, 'kakaomap://route?ep=37.5563,126.9236&by=PUBLICTRANSIT');
    assert.equal(
      links.web,
      `https://map.kakao.com/link/to/${encodeURIComponent('망원동 김반장')},37.5563,126.9236`,
    );
  });

  it('네이버지도: 위도·경도를 뒤집지 않는다 (앱 dlat/dlng, 웹 ex=경도 ey=위도)', () => {
    const links = directionsLinks('naver', dest);
    assert.match(links.app, /^nmap:\/\/route\/public\?dlat=37\.5563&dlng=126\.9236&/);
    assert.match(links.web, /ex=126\.9236&ey=37\.5563/);
  });

  it('가게 이름은 주소에 안전하게 들어간다', () => {
    const links = directionsLinks('kakao', { ...dest, name: 'A&B, 파스타' });
    assert.ok(links.web.includes(encodeURIComponent('A&B, 파스타')));
  });
});

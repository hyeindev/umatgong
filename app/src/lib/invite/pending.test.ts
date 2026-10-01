/// <reference types="node" />
// 순수 로직 테스트. node --test로 돈다 (npm test). Node 타입은 테스트 파일에만 들인다
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { isInviteCode, parsePending, serializePending } from './pending.ts';

const CODE = 'q2Vw_3xT-9aBcDeFgHiJkLmNoPqRsTuV';
const NOW = 1_800_000_000_000;

describe('pending invite', () => {
  it('저장한 코드를 그대로 꺼낸다', () => {
    assert.equal(parsePending(serializePending(CODE, NOW), NOW + 1000), CODE);
  });

  it('한 시간이 지나면 버린다 — 한참 뒤 로그인했을 때 엉뚱하게 합류 화면이 뜨지 않게', () => {
    assert.equal(parsePending(serializePending(CODE, NOW), NOW + 60 * 60_000 + 1), null);
  });

  it('깨진 값이나 다른 형식은 버린다', () => {
    assert.equal(parsePending(null, NOW), null);
    assert.equal(parsePending('not json', NOW), null);
    assert.equal(parsePending(JSON.stringify({ inviteCode: 1, savedAt: NOW }), NOW), null);
    assert.equal(parsePending(JSON.stringify({ inviteCode: CODE }), NOW), null);
  });

  it('초대 코드 형식이 아니면 받지 않는다', () => {
    assert.ok(isInviteCode(CODE));
    assert.ok(!isInviteCode(''));
    assert.ok(!isInviteCode('../login'));
    assert.ok(!isInviteCode('a b'));
    assert.ok(!isInviteCode('x'.repeat(65)));
    assert.equal(parsePending(serializePending('../evil', NOW), NOW), null);
  });
});

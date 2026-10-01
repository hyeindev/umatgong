package com.umatgong.domain.visit.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

/**
 * 내 기록 목록의 커서. (visitedAt, id)를 담는다. 클라이언트는 내용을 해석하지 않고 그대로 돌려준다.
 * offset 대신 커서를 쓰는 이유: 목록 중간에 기록이 추가·삭제돼도 건너뛰거나 겹치지 않는다.
 */
record VisitCursor(Instant visitedAt, long id) {

	String encode() {
		String raw = visitedAt.toString() + "|" + id;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	static VisitCursor decode(String cursor) {
		try {
			String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			int bar = raw.indexOf('|');
			return new VisitCursor(Instant.parse(raw.substring(0, bar)), Long.parseLong(raw.substring(bar + 1)));
		} catch (RuntimeException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "cursor가 올바르지 않습니다.");
		}
	}
}

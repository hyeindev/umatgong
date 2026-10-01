package com.umatgong.domain.visit.dto;

import java.util.List;

/** @param nextCursor 다음 페이지 커서. 더 없으면 null */
public record VisitPageResponse(List<VisitResponse> items, String nextCursor) {
}

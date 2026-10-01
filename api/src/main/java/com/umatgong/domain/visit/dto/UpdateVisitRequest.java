package com.umatgong.domain.visit.dto;

import com.umatgong.domain.visit.entity.Rating;

import jakarta.validation.constraints.Size;

/**
 * 보낸 필드만 바꾼다.
 *
 * @param memo 빈 문자열("")이면 메모를 지운다. 필드를 빼면 그대로 둔다
 */
public record UpdateVisitRequest(Rating rating, @Size(max = 200) String memo) {
}

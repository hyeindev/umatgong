package com.umatgong.domain.visit.dto;

import java.time.Instant;

import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.entity.Visibility;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param clubId       기록을 남길 클럽. 없으면 클럽 없이 나만 보는 기록(PRIVATE)이 된다
 * @param visibility   없으면 클럽이 있을 때 CLUB, 없을 때 PRIVATE
 * @param thumbnailUrl 썸네일 주소 (https). 원본 사진은 서버에 올리지 않는다
 */
public record CreateVisitRequest(
	@NotNull Long placeId,
	Long clubId,
	@NotNull Rating rating,
	@Size(max = 200) String memo,
	@NotNull Instant visitedAt,
	Visibility visibility,
	@Size(max = 500) @Pattern(regexp = "https://\\S+", message = "https 주소여야 합니다") String thumbnailUrl
) {
}

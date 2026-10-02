package com.umatgong.domain.visit.dto;

import java.time.Instant;
import java.util.List;

import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.entity.Visibility;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param clubId        기록을 남길 클럽. 없으면 클럽 없이 나만 보는 기록(PRIVATE)이 된다
 * @param visibility    없으면 클럽이 있을 때 CLUB, 없을 때 PRIVATE
 * @param thumbnailUrls 썸네일 주소 0~3개. POST /api/photos/thumbnails로 내가 올린 주소만 된다.
 *                      원본 사진은 서버에 올리지 않는다
 */
public record CreateVisitRequest(
	@NotNull Long placeId,
	Long clubId,
	@NotNull Rating rating,
	@Size(max = 200) String memo,
	@NotNull Instant visitedAt,
	Visibility visibility,
	@Size(max = 3) List<@NotNull @Size(max = 500) String> thumbnailUrls
) {

	public List<String> thumbnails() {
		return thumbnailUrls == null ? List.of() : thumbnailUrls;
	}
}

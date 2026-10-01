package com.umatgong.domain.visit.dto;

import java.time.Instant;

import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.place.dto.PlaceResponse.CoordinateResponse;
import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.entity.Visibility;

/**
 * 기록 한 건. 목록·상세 공통.
 *
 * @param club           클럽 없이 남긴 기록이면 null
 * @param mine           요청한 사람이 쓴 기록인지 (수정·삭제 버튼 노출 기준)
 * @param distanceMeters 주변 조회일 때만 기준 좌표로부터의 직선거리. 그 밖에는 null
 */
public record VisitResponse(
	Long id,
	PlaceSummary place,
	ClubSummary club,
	Author author,
	Rating rating,
	String memo,
	Visibility visibility,
	Instant visitedAt,
	Instant createdAt,
	String thumbnailUrl,
	boolean mine,
	Integer distanceMeters
) {

	public record PlaceSummary(Long id, String name, String address, String category, CoordinateResponse coordinate) {
	}

	public record ClubSummary(Long id, String name, ClubColor color) {
	}

	public record Author(Long id, String name, String avatarUrl) {
	}
}

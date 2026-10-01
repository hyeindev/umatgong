package com.umatgong.domain.visit.dto;

import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.place.dto.PlaceResponse.CoordinateResponse;
import com.umatgong.domain.visit.entity.Rating;

/**
 * 지도 핀. 가볍게 둔다. 상세는 장소별 기록(GET /api/places/{placeId}/visits)으로 따로 받는다.
 *
 * @param clubColor    클럽 없이 남긴 나만의 기록이면 null
 * @param thumbnailUrl 썸네일 1장. 없으면 null
 */
public record VisitPinResponse(
	Long id,
	Long placeId,
	CoordinateResponse coordinate,
	ClubColor clubColor,
	Rating rating,
	String thumbnailUrl
) {
}

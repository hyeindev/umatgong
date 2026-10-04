package com.umatgong.domain.scrap.dto;

import java.time.Instant;

import com.umatgong.domain.place.dto.PlaceResponse;

/** 내 스크랩 한 건. place의 distanceMeters는 항상 null이다 */
public record ScrapResponse(PlaceResponse place, Instant scrappedAt) {
}

package com.umatgong.domain.scrap.dto;

/** @param scrapped 내가 이 장소를 스크랩했는지 */
public record ScrapStatusResponse(Long placeId, boolean scrapped) {
}

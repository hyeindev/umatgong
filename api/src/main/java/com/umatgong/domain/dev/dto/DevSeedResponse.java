package com.umatgong.domain.dev.dto;

/**
 * @param created      이번 호출로 새로 만들었으면 true. 이미 시드가 있어 그대로 돌려줬으면 false
 * @param placeCount   시드 클럽에 기록이 있는 장소 수
 * @param visitCount   시드 클럽에 남긴 내 기록 수
 */
public record DevSeedResponse(Long clubId, String clubName, int placeCount, int visitCount, boolean created) {
}

package com.umatgong.domain.photo.dto;

/** @param url 기록을 남길 때 thumbnailUrls에 그대로 넣는 주소 */
public record ThumbnailResponse(String url, int width, int height) {
}

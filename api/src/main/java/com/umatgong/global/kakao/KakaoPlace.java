package com.umatgong.global.kakao;

import java.util.Arrays;
import java.util.List;

import com.umatgong.global.geo.Coordinate;

/**
 * 카카오 장소 검색 결과 한 건. 좌표는 이미 lat/lng로 바뀌어 있다 (카카오의 x/y는 밖으로 나오지 않는다).
 *
 * @param externalId        카카오 장소 ID
 * @param categoryName      카카오 분류 전체 경로. 예: "음식점 > 한식 > 육류,고기 > 곱창,막창"
 * @param categoryGroupCode FD6(음식점) / CE7(카페) 등
 */
public record KakaoPlace(
	String externalId,
	String name,
	String address,
	String categoryName,
	String categoryGroupCode,
	Coordinate coordinate
) {

	/**
	 * 업종 태그. 분류 경로에서 최상위("음식점" 등)를 뺀 나머지 단계다.
	 * 예: "음식점 > 한식 > 육류,고기 > 곱창,막창" → [한식, 육류,고기, 곱창,막창]
	 */
	public List<String> categoryTags() {
		if (categoryName == null || categoryName.isBlank()) {
			return List.of();
		}
		List<String> parts = Arrays.stream(categoryName.split(">")).map(String::strip).filter(s -> !s.isEmpty())
			.toList();
		return parts.size() <= 1 ? parts : parts.subList(1, parts.size());
	}
}

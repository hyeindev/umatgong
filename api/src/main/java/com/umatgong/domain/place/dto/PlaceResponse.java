package com.umatgong.domain.place.dto;

import com.umatgong.domain.place.entity.Place;
import com.umatgong.global.geo.Coordinate;

/**
 * @param category       업종의 가장 구체적인 단계. 예: "곱창,막창"
 * @param distanceMeters 요청한 중심 좌표로부터의 직선거리. 중심이 없으면 null
 * @param clubId         직접 입력한 클럽 전용 장소면 그 클럽 ID. 카카오 장소면 null.
 *                       클럽 전용 장소에는 그 클럽으로만 기록할 수 있다
 */
public record PlaceResponse(
	Long id,
	String name,
	String address,
	String category,
	CoordinateResponse coordinate,
	Integer distanceMeters,
	Long clubId
) {

	public static PlaceResponse of(Place place, Coordinate center) {
		Coordinate c = place.getCoordinate();
		Integer distance = center == null ? null : (int) Math.round(center.distanceMetersTo(c));
		return new PlaceResponse(place.getId(), place.getName(), place.getAddress(), leafCategory(place.getCategory()),
			new CoordinateResponse(c.lat(), c.lng()), distance, place.isCustom() ? place.getClub().getId() : null);
	}

	private static String leafCategory(String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return null;
		}
		String[] parts = categoryName.split(">");
		return parts[parts.length - 1].strip();
	}

	public record CoordinateResponse(double lat, double lng) {
	}
}

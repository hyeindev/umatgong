package com.umatgong.global.geo;

import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 도메인 전체에서 쓰는 좌표. 밖으로는 lat/lng만 보이고, DB에는 geography(Point, 4326)로 저장된다.
 *
 * <p>컬럼 이름은 쓰는 쪽에서 {@code @AttributeOverride(name = "point", ...)}로 정한다.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coordinate {

	private static final int WGS84 = 4326;
	private static final double EARTH_RADIUS_METERS = 6_371_008.8;
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84);

	@JdbcTypeCode(SqlTypes.GEOGRAPHY)
	@Column(name = "coordinate")
	private Point point;

	private Coordinate(Point point) {
		this.point = point;
	}

	public static Coordinate of(double lat, double lng) {
		if (lat < -90 || lat > 90) {
			throw new IllegalArgumentException("lat out of range: " + lat);
		}
		if (lng < -180 || lng > 180) {
			throw new IllegalArgumentException("lng out of range: " + lng);
		}
		// JTS와 PostGIS는 x=경도, y=위도다. 여기서 순서를 바꾸면 모든 핀이 뒤집힌다.
		return new Coordinate(GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(lng, lat)));
	}

	public double lat() {
		return point.getY();
	}

	public double lng() {
		return point.getX();
	}

	/** 두 좌표 사이의 지표면 거리(미터, 하버사인). 목록의 "여기서 200m" 같은 표시용이다. */
	public double distanceMetersTo(Coordinate other) {
		double dLat = Math.toRadians(other.lat() - lat());
		double dLng = Math.toRadians(other.lng() - lng());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat())) * Math.cos(Math.toRadians(other.lat()))
			* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
	}

	Point toPoint() {
		return point;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Coordinate other)) {
			return false;
		}
		return Double.compare(lat(), other.lat()) == 0 && Double.compare(lng(), other.lng()) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lat(), lng());
	}

	@Override
	public String toString() {
		return "Coordinate(lat=" + lat() + ", lng=" + lng() + ")";
	}
}

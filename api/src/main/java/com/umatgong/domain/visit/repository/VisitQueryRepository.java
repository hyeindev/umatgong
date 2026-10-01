package com.umatgong.domain.visit.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.place.dto.PlaceResponse.CoordinateResponse;
import com.umatgong.domain.visit.dto.VisitPinResponse;
import com.umatgong.domain.visit.dto.VisitResponse;
import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.entity.Visibility;
import com.umatgong.global.geo.Coordinate;

import lombok.RequiredArgsConstructor;

/**
 * 기록 조회. 공간 조건(bounding box, ST_DWithin)이 있어 SQL로 직접 쓴다.
 *
 * <p><b>모든 조회는 {@link #VISIBLE}을 거친다.</b> 이 조건이 클럽 경계다. 호출하는 쪽은 요청자가 지금 속한
 * 클럽 ID 목록을 넘긴다 (ClubMemberRepository#findClubIdsByUserId). 조건을 빠뜨린 조회를 만들지 않도록
 * 공개 메서드는 모두 viewerId와 clubIds를 받는다.
 */
@Repository
@RequiredArgsConstructor
public class VisitQueryRepository {

	/**
	 * 요청자에게 보이는 기록.
	 * <ul>
	 *   <li>클럽 기록: 요청자가 지금 그 클럽 멤버이고, CLUB 공개이거나 본인이 쓴 것</li>
	 *   <li>클럽 없는 기록: 본인이 쓴 것</li>
	 * </ul>
	 * 속하지 않은 클럽의 기록은 본인이 쓴 것이라도 내보내지 않는다 (탈퇴한 클럽 포함).
	 */
	static final String VISIBLE = """
		((v.club_id = any(:clubIds) and (v.visibility = 'CLUB' or v.user_id = :viewerId))
		 or (v.club_id is null and v.user_id = :viewerId))""";

	// 지도 영역. geography의 && 연산자는 GIST 인덱스(ix_places_coordinate)를 탄다.
	// ST_MakeEnvelope(xmin, ymin, xmax, ymax) — x가 경도다.
	static final String IN_BOUNDS =
		"p.coordinate && ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)::geography";

	// 반경. ST_DWithin(geography)은 GIST 인덱스를 타고 미터 단위로 계산한다. ST_MakePoint(경도, 위도).
	static final String WITHIN_RADIUS =
		"ST_DWithin(p.coordinate, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)";

	private static final String FIRST_THUMB =
		"(select ph.thumb_url from visit_photos ph where ph.visit_id = v.id order by ph.id limit 1)";

	static final String PIN_SQL = """
		select v.id, v.place_id, v.rating, c.color as club_color,
		       ST_Y(p.coordinate::geometry) as lat, ST_X(p.coordinate::geometry) as lng,
		       %s as thumb
		from visits v
		join places p on p.id = v.place_id
		left join clubs c on c.id = v.club_id
		where %s and %s and (:filterClubs = false or v.club_id = any(:filterClubIds))
		order by v.visited_at desc, v.id desc
		limit :limit
		""".formatted(FIRST_THUMB, IN_BOUNDS, VISIBLE);

	private static final String DETAIL_SELECT = """
		select v.id, v.rating, v.memo, v.visibility, v.visited_at, v.created_at, v.user_id,
		       u.name as user_name, u.avatar_url,
		       p.id as place_id, p.name as place_name, p.address, p.category,
		       ST_Y(p.coordinate::geometry) as lat, ST_X(p.coordinate::geometry) as lng,
		       c.id as club_id, c.name as club_name, c.color as club_color,
		       %s as thumb, %s as distance
		from visits v
		join places p on p.id = v.place_id
		join users u on u.id = v.user_id
		left join clubs c on c.id = v.club_id
		""";

	static final String NEARBY_SQL = DETAIL_SELECT.formatted(FIRST_THUMB,
		"ST_Distance(p.coordinate, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)") + """
		where %s and %s and (:filterRatings = false or v.rating = any(:ratings))
		order by distance, v.visited_at desc, v.id desc
		limit :limit
		""".formatted(WITHIN_RADIUS, VISIBLE);

	private static final String BY_PLACE_SQL = DETAIL_SELECT.formatted(FIRST_THUMB, "null") + """
		where v.place_id = :placeId and %s
		order by v.visited_at desc, v.id desc
		limit :limit
		""".formatted(VISIBLE);

	private static final String MINE_SQL = DETAIL_SELECT.formatted(FIRST_THUMB, "null") + """
		where v.user_id = :viewerId and %s
		  and (:afterCursor = false or (v.visited_at, v.id) < (:cursorVisitedAt, :cursorId))
		order by v.visited_at desc, v.id desc
		limit :limit
		""".formatted(VISIBLE);

	private static final String BY_ID_SQL = DETAIL_SELECT.formatted(FIRST_THUMB, "null") + """
		where v.id = :id and %s
		""".formatted(VISIBLE);

	private final NamedParameterJdbcTemplate jdbc;

	public List<VisitPinResponse> pins(Long viewerId, Collection<Long> clubIds, Coordinate sw, Coordinate ne,
		Collection<Long> filterClubIds, int limit) {
		MapSqlParameterSource params = visibility(viewerId, clubIds)
			.addValue("swLat", sw.lat()).addValue("swLng", sw.lng())
			.addValue("neLat", ne.lat()).addValue("neLng", ne.lng())
			.addValue("filterClubs", filterClubIds != null)
			.addValue("filterClubIds", longs(filterClubIds == null ? List.of() : filterClubIds))
			.addValue("limit", limit);
		return jdbc.query(PIN_SQL, params, (rs, i) -> new VisitPinResponse(
			rs.getLong("id"),
			rs.getLong("place_id"),
			new CoordinateResponse(rs.getDouble("lat"), rs.getDouble("lng")),
			clubColor(rs),
			Rating.valueOf(rs.getString("rating")),
			rs.getString("thumb")));
	}

	public List<VisitResponse> nearby(Long viewerId, Collection<Long> clubIds, Coordinate center, int radiusMeters,
		Collection<Rating> ratings, int limit) {
		MapSqlParameterSource params = visibility(viewerId, clubIds)
			.addValue("lat", center.lat()).addValue("lng", center.lng())
			.addValue("radius", radiusMeters)
			.addValue("filterRatings", ratings != null && !ratings.isEmpty())
			.addValue("ratings", ratings == null ? new String[0] : ratings.stream().map(Enum::name).toArray(String[]::new))
			.addValue("limit", limit);
		return jdbc.query(NEARBY_SQL, params, detailMapper(viewerId));
	}

	public List<VisitResponse> byPlace(Long viewerId, Collection<Long> clubIds, Long placeId, int limit) {
		MapSqlParameterSource params = visibility(viewerId, clubIds)
			.addValue("placeId", placeId)
			.addValue("limit", limit);
		return jdbc.query(BY_PLACE_SQL, params, detailMapper(viewerId));
	}

	/** 내 기록. (visitedAt, id) 커서 기준으로 그보다 오래된 것부터 */
	public List<VisitResponse> mine(Long viewerId, Collection<Long> clubIds, Instant cursorVisitedAt, Long cursorId,
		int limit) {
		boolean afterCursor = cursorVisitedAt != null && cursorId != null;
		MapSqlParameterSource params = visibility(viewerId, clubIds)
			.addValue("afterCursor", afterCursor)
			.addValue("cursorVisitedAt", OffsetDateTime.ofInstant(afterCursor ? cursorVisitedAt : Instant.EPOCH, ZoneOffset.UTC))
			.addValue("cursorId", afterCursor ? cursorId : 0L)
			.addValue("limit", limit);
		return jdbc.query(MINE_SQL, params, detailMapper(viewerId));
	}

	public Optional<VisitResponse> findVisible(Long viewerId, Collection<Long> clubIds, Long visitId) {
		MapSqlParameterSource params = visibility(viewerId, clubIds).addValue("id", visitId);
		return jdbc.query(BY_ID_SQL, params, detailMapper(viewerId)).stream().findFirst();
	}

	private static MapSqlParameterSource visibility(Long viewerId, Collection<Long> clubIds) {
		return new MapSqlParameterSource()
			.addValue("viewerId", viewerId)
			.addValue("clubIds", longs(clubIds));
	}

	// = any(:param)에는 배열 하나로 넘긴다. 빈 목록도 그대로 동작한다 (아무것도 일치하지 않음).
	private static Long[] longs(Collection<Long> values) {
		return values.toArray(Long[]::new);
	}

	private static ClubColor clubColor(ResultSet rs) throws SQLException {
		String color = rs.getString("club_color");
		return color == null ? null : ClubColor.valueOf(color);
	}

	private static RowMapper<VisitResponse> detailMapper(Long viewerId) {
		return (rs, i) -> {
			long clubId = rs.getLong("club_id");
			boolean hasClub = !rs.wasNull();
			double distance = rs.getDouble("distance");
			boolean hasDistance = !rs.wasNull();
			long authorId = rs.getLong("user_id");
			return new VisitResponse(
				rs.getLong("id"),
				new VisitResponse.PlaceSummary(rs.getLong("place_id"), rs.getString("place_name"),
					rs.getString("address"), leafCategory(rs.getString("category")),
					new CoordinateResponse(rs.getDouble("lat"), rs.getDouble("lng"))),
				hasClub ? new VisitResponse.ClubSummary(clubId, rs.getString("club_name"), clubColor(rs)) : null,
				new VisitResponse.Author(authorId, rs.getString("user_name"), rs.getString("avatar_url")),
				Rating.valueOf(rs.getString("rating")),
				rs.getString("memo"),
				Visibility.valueOf(rs.getString("visibility")),
				instant(rs, "visited_at"),
				instant(rs, "created_at"),
				rs.getString("thumb"),
				viewerId.equals(authorId),
				hasDistance ? (int) Math.round(distance) : null);
		};
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
		return value == null ? null : value.toInstant();
	}

	// 장소 응답(PlaceResponse)과 같은 규칙: 카카오 업종의 가장 구체적인 단계만
	private static String leafCategory(String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return null;
		}
		String[] parts = categoryName.split(">");
		return parts[parts.length - 1].strip();
	}
}

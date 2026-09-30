package com.umatgong.domain.place.repository;

import java.sql.Array;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.umatgong.global.kakao.KakaoPlace;

import lombok.RequiredArgsConstructor;

/**
 * 카카오 장소를 places에 upsert 한다. 같은 카카오 장소는 external_id로 한 행에 모인다.
 *
 * <p>"조회 후 없으면 저장"을 JPA로 하면 같은 장소를 동시에 저장할 때 중복 오류가 난다.
 * INSERT ... ON CONFLICT로 DB가 한 번에 처리하게 해 경합이 생기지 않는다.
 * 부분 유니크 인덱스(uq_places_external_id, source = 'API')를 쓰므로 ON CONFLICT에도 같은 조건을 준다.
 */
@Repository
@RequiredArgsConstructor
public class KakaoPlaceUpsertRepository {

	private static final String UPSERT_SQL = """
		insert into places (name, address, coordinate, category, source, external_id, category_tags)
		values (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, 'API', ?, ?)
		on conflict (external_id) where source = 'API'
		do update set name = excluded.name,
		              address = excluded.address,
		              coordinate = excluded.coordinate,
		              category = excluded.category,
		              category_tags = excluded.category_tags
		""";

	private final JdbcTemplate jdbcTemplate;

	public void upsertAll(List<KakaoPlace> places) {
		jdbcTemplate.batchUpdate(UPSERT_SQL, places, places.size(), (ps, place) -> {
			ps.setString(1, truncate(place.name(), 100));
			ps.setString(2, truncate(place.address(), 255));
			// ST_MakePoint(경도, 위도) — 경도가 먼저다.
			ps.setDouble(3, place.coordinate().lng());
			ps.setDouble(4, place.coordinate().lat());
			ps.setString(5, truncate(place.categoryName(), 100));
			ps.setString(6, place.externalId());
			Array tags = ps.getConnection().createArrayOf("text", place.categoryTags().toArray());
			ps.setArray(7, tags);
		});
	}

	// 카카오 데이터가 컬럼 길이를 넘으면 500 대신 잘라서 저장한다.
	private static String truncate(String value, int max) {
		return value == null || value.length() <= max ? value : value.substring(0, max);
	}
}

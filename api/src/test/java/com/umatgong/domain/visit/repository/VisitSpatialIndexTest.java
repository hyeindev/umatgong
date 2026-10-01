package com.umatgong.domain.visit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 지도·주변 조회의 공간 조건이 GIST 인덱스(ix_places_coordinate)를 쓸 수 있는 형태인지 확인한다.
 *
 * <p>실제 쿼리에 쓰는 조건 문자열(IN_BOUNDS, WITHIN_RADIUS)을 그대로 places에 걸어 실행 계획을 본다.
 * 전체 쿼리로 보면 테스트 DB처럼 행이 거의 없을 때 플래너가 다른 인덱스부터 읽는 계획을 고를 수 있어
 * 결과가 데이터에 따라 흔들린다. 여기서 보려는 것은 “조건이 인덱스를 못 타는 모양(예: 컬럼에 함수를 씌움)으로
 * 쓰이지 않았는가”이고, 그건 조건만으로 확인된다. 행이 적어 순차 스캔이 골라지지 않도록 enable_seqscan을 끈다.
 */
@SpringBootTest
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class VisitSpatialIndexTest {

	@Autowired
	private NamedParameterJdbcTemplate jdbc;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void 지도_영역_조회는_GIST_인덱스를_탄다() {
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("swLat", 37.4).addValue("swLng", 126.7).addValue("neLat", 37.7).addValue("neLng", 127.2);

		assertThat(plan(VisitQueryRepository.IN_BOUNDS, params)).contains("Index Scan using ix_places_coordinate");
	}

	@Test
	void 주변_조회는_GIST_인덱스를_탄다() {
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("lat", 37.5563).addValue("lng", 126.9236).addValue("radius", 1000);

		assertThat(plan(VisitQueryRepository.WITHIN_RADIUS, params)).contains("Index Scan using ix_places_coordinate");
	}

	private String plan(String placeCondition, MapSqlParameterSource params) {
		return new TransactionTemplate(transactionManager).execute(status -> {
			jdbc.getJdbcTemplate().execute("set local enable_seqscan = off");
			List<String> lines = jdbc.queryForList("explain select p.id from places p where " + placeCondition,
				params, String.class);
			return String.join("\n", lines);
		});
	}
}

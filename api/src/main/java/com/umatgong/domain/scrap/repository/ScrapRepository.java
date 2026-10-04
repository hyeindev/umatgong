package com.umatgong.domain.scrap.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 스크랩. 행 하나가 (사용자, 장소) 쌍이라 엔티티 없이 SQL로 다룬다.
 * 추가·해제는 여러 번 해도 결과가 같다 (ON CONFLICT DO NOTHING / 없는 행 DELETE).
 */
@Repository
@RequiredArgsConstructor
public class ScrapRepository {

	/**
	 * 내 스크랩. 클럽 전용 장소는 내가 지금 그 클럽 멤버일 때만 (탈퇴한 클럽의 장소는 목록에서 빠진다).
	 */
	private static final String MINE_SQL = """
		select s.place_id, s.created_at
		from scraps s
		join places p on p.id = s.place_id
		where s.user_id = :userId
		  and (p.club_id is null or p.club_id = any(:clubIds))
		order by s.created_at desc, s.place_id desc
		limit :limit
		""";

	private final NamedParameterJdbcTemplate jdbc;

	public void add(Long userId, Long placeId) {
		jdbc.update("insert into scraps (user_id, place_id) values (:userId, :placeId) on conflict do nothing",
			params(userId, placeId));
	}

	public void remove(Long userId, Long placeId) {
		jdbc.update("delete from scraps where user_id = :userId and place_id = :placeId", params(userId, placeId));
	}

	public boolean exists(Long userId, Long placeId) {
		Boolean found = jdbc.queryForObject(
			"select exists(select 1 from scraps where user_id = :userId and place_id = :placeId)",
			params(userId, placeId), Boolean.class);
		return Boolean.TRUE.equals(found);
	}

	public List<Row> mine(Long userId, Collection<Long> clubIds, int limit) {
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("userId", userId)
			.addValue("clubIds", clubIds.toArray(Long[]::new))
			.addValue("limit", limit);
		return jdbc.query(MINE_SQL, params,
			(rs, i) -> new Row(rs.getLong("place_id"), rs.getObject("created_at", Timestamp.class).toInstant()));
	}

	private static MapSqlParameterSource params(Long userId, Long placeId) {
		return new MapSqlParameterSource().addValue("userId", userId).addValue("placeId", placeId);
	}

	public record Row(Long placeId, Instant scrappedAt) {
	}
}

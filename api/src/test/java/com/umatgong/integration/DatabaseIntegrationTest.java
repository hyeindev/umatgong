package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.umatgong.domain.auth.entity.RefreshToken;
import com.umatgong.domain.auth.repository.RefreshTokenRepository;
import com.umatgong.domain.club.entity.Club;
import com.umatgong.domain.club.entity.ClubColor;
import com.umatgong.domain.place.entity.Place;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.entity.TagSource;
import com.umatgong.domain.visit.entity.Visibility;
import com.umatgong.domain.visit.entity.Visit;
import com.umatgong.domain.visit.entity.VisitPhoto;
import com.umatgong.global.geo.Coordinate;
import com.umatgong.global.security.JwtProvider;

import jakarta.persistence.EntityManager;

/**
 * 실제 PostGIS에 붙여 전체 컨텍스트를 띄운다. 런타임에만 드러나는 것을 여기서 잡는다:
 * Flyway 적용, Hibernate 스키마 검증(ddl-auto=validate), geography·text[] 매핑, JWT 발급·검증.
 *
 * <p>CI_INTEGRATION_DB=true일 때만 돈다 (TestDatabase 참고). CI는 이 값을 넣고, 이 테스트가 실제로 돌았는지 따로 확인한다.
 * 각 테스트는 트랜잭션 안에서 돌고 끝나면 롤백된다.
 */
@SpringBootTest
@Transactional
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class DatabaseIntegrationTest {

	private static final Coordinate MANGWON = Coordinate.of(37.5556, 126.9106);
	private static final Coordinate HAPJEONG = Coordinate.of(37.5495, 126.9139);

	@Autowired
	private Flyway flyway;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManager em;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RefreshTokenRepository refreshTokenRepository;
	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void 모든_마이그레이션이_적용되고_대기중인_것이_없다() {
		MigrationInfo[] applied = flyway.info().applied();

		assertThat(Arrays.stream(applied).map(m -> m.getVersion().getVersion())).contains("1", "2");
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(jdbc.queryForObject("select count(*) from pg_extension where extname = 'postgis'", Integer.class))
			.isEqualTo(1);
	}

	@Test
	void 좌표는_경도가_x_위도가_y로_저장되고_그대로_읽힌다() {
		Place place = persist(Place.fromKakao("kakao-1", "망원동 김반장", null, MANGWON, "곱창", List.of("곱창"), null));
		em.clear();

		Double x = jdbc.queryForObject("select ST_X(coordinate::geometry) from places where id = ?", Double.class,
			place.getId());
		Double y = jdbc.queryForObject("select ST_Y(coordinate::geometry) from places where id = ?", Double.class,
			place.getId());
		assertThat(x).isEqualTo(126.9106);
		assertThat(y).isEqualTo(37.5556);

		Place loaded = em.find(Place.class, place.getId());
		assertThat(loaded.getCoordinate()).isEqualTo(MANGWON);
		assertThat(loaded.getCategoryTags()).containsExactly("곱창");
	}

	@Test
	void geography로_저장돼_거리가_미터로_계산된다() {
		Place mangwon = persist(Place.fromKakao("kakao-1", "망원", null, MANGWON, null, List.of(), null));
		Place hapjeong = persist(Place.fromKakao("kakao-2", "합정", null, HAPJEONG, null, List.of(), null));
		em.flush();

		Double meters = jdbc.queryForObject(
			"select ST_Distance(a.coordinate, b.coordinate) from places a, places b where a.id = ? and b.id = ?",
			Double.class, mangwon.getId(), hapjeong.getId());
		// 두 지점은 약 740m 떨어져 있다. 위경도가 뒤집혀 저장됐다면 전혀 다른 값이 나온다.
		assertThat(meters).isBetween(600.0, 900.0);
	}

	@Test
	void 사진_태그와_위치없는_사진이_저장된다() {
		User user = persist(User.signUpWithKakao(1L, "지현", null));
		Place place = persist(Place.fromKakao("kakao-1", "망원동 김반장", null, MANGWON, null, List.of(), null));
		Visit visit = persist(Visit.record(user, place, null, Rating.AGAIN, null, Instant.now(), Visibility.PRIVATE));
		VisitPhoto photo = VisitPhoto.attach(visit, "https://thumb/1.jpg", Instant.now(), null);
		photo.applyTags(List.of("곱창", "소주"), TagSource.ON_DEVICE);
		persist(photo);
		em.clear();

		VisitPhoto loaded = em.find(VisitPhoto.class, photo.getId());
		assertThat(loaded.getTags()).containsExactly("곱창", "소주");
		assertThat(loaded.getExifCoordinate()).isNull();
	}

	@Test
	void 카카오_장소가_클럽에_속하면_DB가_거부한다() {
		User user = persist(User.signUpWithKakao(1L, "지현", null));
		Club club = persist(Club.create("동네친구들", ClubColor.SAGE, user, "invite-1"));
		em.flush();

		assertThatThrownBy(() -> jdbc.update("""
				insert into places (name, coordinate, source, external_id, club_id)
				values ('x', ST_MakePoint(126.9, 37.5)::geography, 'API', 'k-9', ?)
				""", club.getId()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 카카오_회원번호로_사용자를_찾는다() {
		persist(User.signUpWithKakao(4321L, "지현", null));
		em.clear();

		assertThat(userRepository.findByKakaoId(4321L)).get().extracting(User::getName).isEqualTo("지현");
		assertThat(userRepository.findByKakaoId(9999L)).isEmpty();
	}

	@Test
	void 리프레시_토큰_family를_한번에_폐기한다() {
		User user = persist(User.signUpWithKakao(1L, "지현", null));
		Instant now = Instant.now();
		RefreshToken first = persist(RefreshToken.issueOnLogin(user, "a".repeat(64), now.plus(Duration.ofDays(14))));
		RefreshToken second = persist(first.rotate("b".repeat(64), now.plus(Duration.ofDays(14)), now));
		em.flush();

		int revoked = refreshTokenRepository.revokeFamily(first.getFamilyId(), now);

		assertThat(revoked).isEqualTo(1);
		assertThat(refreshTokenRepository.findByTokenHashForUpdate("b".repeat(64))).get()
			.extracting(RefreshToken::isRevoked).isEqualTo(true);
		assertThat(second.getFamilyId()).isEqualTo(first.getFamilyId());
	}

	@Test
	void 설정된_시크릿으로_JWT를_발급하고_검증한다() {
		String token = jwtProvider.createAccessToken(42L);

		assertThat(jwtProvider.parseAccessToken(token)).isEqualTo(42L);
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}
}

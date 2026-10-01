package com.umatgong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * application.yml은 커밋되므로 실제 비밀값이 들어가면 그대로 저장소에 남는다.
 * 비밀값 자리는 전부 ${환경변수} 형태여야 하고 기본값도 비어 있어야 한다.
 */
class ApplicationConfigTest {

	private static PropertySource<?> yml;

	@BeforeAll
	static void load() throws IOException {
		List<PropertySource<?>> sources = new YamlPropertySourceLoader()
			.load("application", new ClassPathResource("application.yml"));
		yml = sources.get(0);
	}

	@Test
	void 비밀값은_환경변수로만_받고_기본값이_비어_있다() {
		assertThat(raw("spring.datasource.password")).isEqualTo("${DB_PASSWORD:}");
		assertThat(raw("umatgong.jwt.secret")).isEqualTo("${JWT_SECRET:}");
		assertThat(raw("umatgong.kakao.rest-api-key")).isEqualTo("${KAKAO_REST_API_KEY:}");
		assertThat(raw("umatgong.kakao.client-secret")).isEqualTo("${KAKAO_CLIENT_SECRET:}");
	}

	@Test
	void 배포_플랫폼이_주는_PORT로_뜬다() {
		assertThat(raw("server.port")).isEqualTo("${PORT:8080}");
	}

	@Test
	void DB_접속정보도_환경변수로_받는다() {
		assertThat(raw("spring.datasource.url")).startsWith("${DB_URL:");
		assertThat(raw("spring.datasource.username")).isEqualTo("${DB_USERNAME:}");
	}

	@Test
	void 요금제_제한값은_설정으로_분리돼_있고_기본값은_기획서_9장과_같다() {
		assertThat(raw("umatgong.plan.free.max-club-members")).isEqualTo("${PLAN_FREE_MAX_CLUB_MEMBERS:8}");
		assertThat(raw("umatgong.plan.free.max-clubs-per-user")).isEqualTo("${PLAN_FREE_MAX_CLUBS_PER_USER:1}");
		assertThat(raw("umatgong.plan.paid.max-club-members")).isEqualTo("${PLAN_PAID_MAX_CLUB_MEMBERS:30}");
		assertThat(raw("umatgong.plan.paid.max-clubs-per-user")).isEqualTo("${PLAN_PAID_MAX_CLUBS_PER_USER:3}");
	}

	private static String raw(String key) {
		Object value = yml.getProperty(key);
		return value == null ? null : value.toString();
	}
}

package com.umatgong.integration;

import java.net.URI;
import java.util.Set;

/**
 * 통합 테스트 실행 조건. 통합 테스트는 테이블을 비우므로(TRUNCATE) 운영 DB에서 돌면 데이터가 지워진다.
 *
 * <ul>
 *   <li>CI_INTEGRATION_DB=true가 없으면 건너뛴다. DB_URL만으로는 돌지 않는다
 *       (배포 플랫폼 빌드에는 운영 DB_URL이 들어 있을 수 있다)</li>
 *   <li>켜져 있어도 DB_URL이 localhost가 아니면 테스트를 실패시킨다. 조용히 건너뛰지 않고 멈춘다</li>
 * </ul>
 *
 * <p>통합 테스트 클래스에 {@code @EnabledIf("com.umatgong.integration.TestDatabase#enabled")}로 붙인다.
 */
public final class TestDatabase {

	static final String FLAG = "CI_INTEGRATION_DB";
	private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

	private TestDatabase() {
	}

	public static boolean enabled() {
		return enabled(System.getenv(FLAG), System.getenv("DB_URL"));
	}

	static boolean enabled(String flag, String dbUrl) {
		if (!"true".equals(flag)) {
			return false;
		}
		String host = hostOf(dbUrl);
		if (!LOCAL_HOSTS.contains(host)) {
			throw new IllegalStateException(
				"Refusing to run destructive integration tests against a non-local database: host=" + host);
		}
		return true;
	}

	private static String hostOf(String dbUrl) {
		if (dbUrl == null || !dbUrl.startsWith("jdbc:")) {
			return String.valueOf(dbUrl);
		}
		String host = URI.create(dbUrl.substring("jdbc:".length())).getHost();
		return host == null ? "" : host;
	}
}

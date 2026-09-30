package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.global.security.JwtProvider;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 장소 API를 HTTP → 카카오(MockWebServer) → PostGIS까지 실제로 흘린다.
 *
 * <p>검색 캐시는 컨텍스트 안에 살아 있으므로 테스트마다 서로 다른 좌표·키워드를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PlaceFlowIntegrationTest {

	private static final List<RecordedRequest> KAKAO_REQUESTS = Collections.synchronizedList(new ArrayList<>());
	// 테스트마다 카카오 응답을 바꿔 끼운다. 기본은 빈 결과.
	private static volatile Function<RecordedRequest, MockResponse> kakaoResponder = r -> json(200, "{\"documents\": []}");
	private static final MockWebServer KAKAO = startKakaoStub();

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private JwtProvider jwtProvider;

	private String bearer;

	@BeforeEach
	void setUp() {
		KAKAO_REQUESTS.clear();
		kakaoResponder = r -> json(200, "{\"documents\": []}");
		Long userId = jdbc.queryForObject(
			"insert into users (kakao_id, name) values (1, '지현') returning id", Long.class);
		bearer = "Bearer " + jwtProvider.createAccessToken(userId);
	}

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	// ---- 좌표: 카카오 x/y → places → 응답 --------------------------------------------------

	@Test
	void 카카오의_x_y가_경도_위도로_저장되고_그대로_돌아온다() throws Exception {
		kakaoResponder = byCategory(
			docs(doc("k-100", "망원동 김반장", "FD6", "음식점 > 한식 > 곱창,막창", "126.9106", "37.5556")),
			docs());

		JsonNode place = nearby(37.5550, 126.9100, 500).get(0);

		// PostGIS: ST_X = 경도, ST_Y = 위도
		assertThat(jdbc.queryForObject("select ST_X(coordinate::geometry) from places where external_id = 'k-100'",
			Double.class)).isEqualTo(126.9106);
		assertThat(jdbc.queryForObject("select ST_Y(coordinate::geometry) from places where external_id = 'k-100'",
			Double.class)).isEqualTo(37.5556);
		// 응답: lat = 카카오 y, lng = 카카오 x
		assertThat(place.at("/coordinate/lat").asDouble()).isEqualTo(37.5556);
		assertThat(place.at("/coordinate/lng").asDouble()).isEqualTo(126.9106);
		assertThat(place.get("category").asText()).isEqualTo("곱창,막창");
		assertThat(place.get("distanceMeters").asInt()).isBetween(50, 120);
	}

	@Test
	void 요청_좌표도_x에_경도_y에_위도로_카카오에_나간다() throws Exception {
		nearby(37.5601, 126.9201, 300);

		assertThat(KAKAO_REQUESTS).hasSize(2).allSatisfy(r -> {
			assertThat(r.getUrl().queryParameter("x")).isEqualTo("126.9201");
			assertThat(r.getUrl().queryParameter("y")).isEqualTo("37.5601");
		});
	}

	@Test
	void 업종_태그가_배열로_저장된다() throws Exception {
		kakaoResponder = byCategory(
			docs(doc("k-200", "합정 국밥", "FD6", "음식점 > 한식 > 국밥", "126.9139", "37.5495")), docs());

		nearby(37.5701, 126.9301, 500);

		String tags = jdbc.queryForObject("select array_to_string(category_tags, '|') from places "
			+ "where external_id = 'k-200'", String.class);
		assertThat(tags).isEqualTo("한식|국밥");
	}

	// ---- 캐시 ------------------------------------------------------------------------------

	@Test
	void 같은_좌표로_두_번_검색하면_두_번째는_카카오를_호출하지_않는다() throws Exception {
		kakaoResponder = byCategory(
			docs(doc("k-300", "음식점", "FD6", "음식점 > 한식", "126.9401", "37.5801")),
			docs(doc("k-301", "카페", "CE7", "음식점 > 카페", "126.9402", "37.5802")));

		JsonNode first = nearby(37.5800, 126.9400, 500);
		int afterFirst = KAKAO_REQUESTS.size();
		JsonNode second = nearby(37.5800, 126.9400, 500);

		// 첫 검색은 음식점(FD6)·카페(CE7) 두 번, 두 번째 검색은 0번
		assertThat(afterFirst).isEqualTo(2);
		assertThat(KAKAO_REQUESTS).hasSize(afterFirst);
		assertThat(second).isEqualTo(first);
	}

	@Test
	void 반올림해서_같은_좌표면_캐시를_쓰고_반경이_다르면_다시_조회한다() throws Exception {
		nearby(37.59001, 126.95001, 500);
		assertThat(KAKAO_REQUESTS).hasSize(2);

		// 소수점 넷째 자리까지 같으면(약 11m 이내) 같은 검색
		nearby(37.59004, 126.95004, 500);
		assertThat(KAKAO_REQUESTS).hasSize(2);

		nearby(37.59001, 126.95001, 1000);
		assertThat(KAKAO_REQUESTS).hasSize(4);
	}

	@Test
	void 같은_키워드로_두_번_검색하면_카카오를_한_번만_호출한다() throws Exception {
		kakaoResponder = r -> json(200, docs(doc("k-400", "망원 곱창", "FD6", "음식점 > 한식", "126.9106", "37.5556")));

		search("망원곱창-캐시", 37.5556, 126.9106).andExpect(status().isOk());
		search("망원곱창-캐시", 37.5000, 126.9000).andExpect(status().isOk());

		assertThat(KAKAO_REQUESTS).hasSize(1);
	}

	@Test
	void 카카오_오류는_캐시되지_않는다() throws Exception {
		kakaoResponder = r -> json(500, "{}");
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.6001").param("lng", "126.9601")
				.header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.error.code").value("KAKAO_UNAVAILABLE"));

		kakaoResponder = r -> json(200, "{\"documents\": []}");
		nearby(37.6001, 126.9601, 500);

		assertThat(KAKAO_REQUESTS).hasSizeGreaterThan(1);
	}

	// ---- upsert ----------------------------------------------------------------------------

	@Test
	void 같은_카카오_장소가_여러_검색에_나와도_한_행이고_최신_정보로_갱신된다() throws Exception {
		kakaoResponder = byCategory(docs(doc("k-500", "옛 이름", "FD6", "음식점 > 한식", "126.9701", "37.6101")), docs());
		nearby(37.6100, 126.9700, 500);
		long id = jdbc.queryForObject("select id from places where external_id = 'k-500'", Long.class);

		kakaoResponder = byCategory(docs(doc("k-500", "새 이름", "FD6", "음식점 > 한식", "126.9701", "37.6101")), docs());
		nearby(37.6200, 126.9800, 500);

		assertThat(jdbc.queryForObject("select count(*) from places where external_id = 'k-500'", Integer.class))
			.isEqualTo(1);
		assertThat(jdbc.queryForObject("select name from places where id = ?", String.class, id)).isEqualTo("새 이름");
	}

	@Test
	void 주변_검색은_음식점과_카페를_합쳐_가까운_순으로_돌려준다() throws Exception {
		kakaoResponder = byCategory(
			docs(doc("k-601", "먼 음식점", "FD6", "음식점 > 한식", "126.9950", "37.6300")),
			docs(doc("k-602", "가까운 카페", "CE7", "음식점 > 카페", "126.9901", "37.6301")));

		JsonNode places = nearby(37.6300, 126.9900, 1000);

		assertThat(places).extracting(p -> p.get("name").asText()).containsExactly("가까운 카페", "먼 음식점");
	}

	@Test
	void 키워드_검색은_음식점과_카페만_돌려준다() throws Exception {
		kakaoResponder = r -> json(200, docs(
			doc("k-701", "망원 곱창", "FD6", "음식점 > 한식", "126.9106", "37.5556"),
			doc("k-702", "망원 주차장", "PK6", "교통 > 주차장", "126.9107", "37.5557"),
			doc("k-703", "망원 커피", "CE7", "음식점 > 카페", "126.9108", "37.5558")));

		String body = search("망원-필터", null, null).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		JsonNode data = objectMapper.readTree(body).get("data");
		assertThat(data).extracting(p -> p.get("name").asText()).containsExactly("망원 곱창", "망원 커피");
		assertThat(data.get(0).get("distanceMeters").isNull()).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from places where external_id = 'k-702'", Integer.class))
			.isZero();
	}

	@Test
	void 로그인하지_않으면_카카오를_호출하지_않는다() throws Exception {
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5").param("lng", "126.9"))
			.andExpect(status().isUnauthorized());

		assertThat(KAKAO_REQUESTS).isEmpty();
	}

	// ---- helpers ---------------------------------------------------------------------------

	private JsonNode nearby(double lat, double lng, int radius) throws Exception {
		String body = mockMvc.perform(get("/api/places/nearby")
				.param("lat", String.valueOf(lat)).param("lng", String.valueOf(lng))
				.param("radius", String.valueOf(radius))
				.header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("data");
	}

	private ResultActions search(String query, Double lat, Double lng) throws Exception {
		var request = get("/api/places/search").param("query", query).header(HttpHeaders.AUTHORIZATION, bearer);
		if (lat != null) {
			request.param("lat", String.valueOf(lat)).param("lng", String.valueOf(lng));
		}
		return mockMvc.perform(request);
	}

	private static Function<RecordedRequest, MockResponse> byCategory(String restaurants, String cafes) {
		return r -> json(200, "CE7".equals(r.getUrl().queryParameter("category_group_code")) ? cafes : restaurants);
	}

	private static String doc(String id, String name, String group, String category, String x, String y) {
		return "{\"id\": \"" + id + "\", \"place_name\": \"" + name + "\", \"category_name\": \"" + category + "\", "
			+ "\"category_group_code\": \"" + group + "\", \"address_name\": \"서울 마포구\", "
			+ "\"road_address_name\": \"\", \"x\": \"" + x + "\", \"y\": \"" + y + "\"}";
	}

	private static String docs(String... docs) {
		return "{\"documents\": [" + String.join(",", docs) + "]}";
	}

	private static MockResponse json(int code, String body) {
		return new MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build();
	}

	private static MockWebServer startKakaoStub() {
		MockWebServer server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				KAKAO_REQUESTS.add(request);
				return kakaoResponder.apply(request);
			}
		});
		try {
			server.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return server;
	}

	@TestConfiguration
	static class KakaoStubConfig {

		@Bean
		@Primary
		@Qualifier("kakaoLocalRestClient")
		RestClient kakaoLocalStubRestClient() {
			return RestClient.builder().baseUrl(KAKAO.url("/").toString()).build();
		}
	}
}

package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.domain.dev.controller.DevSeedController;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.security.JwtProvider;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 개발용 시드 API (플래그 켜짐). 카카오는 MockWebServer로 흉내 내고 DB(PostGIS)까지 흘린다.
 *
 * <p>카카오 응답: 광교중앙역 검색 12곳, 상현역 검색 12곳 (그중 2곳은 겹침) → 서로 다른 22곳 중 20곳을 쓴다.
 */
@SpringBootTest(properties = {
	"umatgong.dev.seed.enabled=true",
	"umatgong.dev.seed.token=" + DevSeedFlowIntegrationTest.TOKEN
})
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class DevSeedFlowIntegrationTest {

	static final String TOKEN = "test-dev-seed-token-0123456789abcdef";

	private static final List<RecordedRequest> KAKAO_REQUESTS = Collections.synchronizedList(new ArrayList<>());
	private static final MockWebServer KAKAO = startKakaoStub();

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtProvider jwtProvider;

	private long kakaoIdSeq = 1;
	private long userId;
	private String jihyun;

	@BeforeEach
	void setUp() {
		KAKAO_REQUESTS.clear();
		User user = userRepository.save(User.signUpWithKakao(kakaoIdSeq++, "지현", null));
		userId = user.getId();
		jihyun = jwtProvider.createAccessToken(userId);
	}

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	// ── 생성 ──

	@Test
	void 클럽_하나와_광교_음식점_기록을_만든다() throws Exception {
		JsonNode seeded = data(seed(jihyun, TOKEN).andExpectOk());

		assertThat(seeded.get("created").asBoolean()).isTrue();
		assertThat(seeded.get("placeCount").asInt()).isEqualTo(20);
		assertThat(seeded.get("visitCount").asInt()).isEqualTo(20);
		long clubId = seeded.get("clubId").asLong();

		// 내가 클럽장이자 유일한 멤버인 클럽
		assertThat(jdbc.queryForObject("select created_by from clubs where id = ?", Long.class, clubId))
			.isEqualTo(userId);
		assertThat(jdbc.queryForList("select user_id from club_members where club_id = ?", Long.class, clubId))
			.containsExactly(userId);
		// 광교중앙역·상현역 두 곳을 음식점(FD6)으로 검색했다. 좌표는 x=경도, y=위도로 나갔다
		assertThat(KAKAO_REQUESTS).hasSize(2).allSatisfy(r -> {
			assertThat(r.getUrl().queryParameter("category_group_code")).isEqualTo("FD6");
			assertThat(Double.parseDouble(r.getUrl().queryParameter("y"))).isBetween(37.2, 37.4);
			assertThat(Double.parseDouble(r.getUrl().queryParameter("x"))).isBetween(127.0, 127.1);
		});
		// places에 카카오 장소로 저장됐고 위도·경도가 뒤집히지 않았다
		assertThat(jdbc.queryForObject("select count(*) from places where source = 'API'", Integer.class))
			.isEqualTo(20);
		assertThat(jdbc.queryForObject(
			"select count(*) from places where ST_Y(coordinate::geometry) between 37.2 and 37.4 "
				+ "and ST_X(coordinate::geometry) between 127.0 and 127.1", Integer.class)).isEqualTo(20);
		// 장소마다 기록 하나, 전부 클럽 공개, 사진 없음
		assertThat(jdbc.queryForObject("select count(distinct place_id) from visits where club_id = ?", Integer.class,
			clubId)).isEqualTo(20);
		assertThat(jdbc.queryForList("select distinct visibility from visits", String.class)).containsExactly("CLUB");
		assertThat(jdbc.queryForObject("select count(*) from visit_photos", Integer.class)).isZero();
	}

	@Test
	void 평가는_60_30_10_메모는_절반_방문_시각은_최근_6개월() throws Exception {
		seed(jihyun, TOKEN).andExpectOk();

		Map<String, Long> ratings = jdbc.queryForList("select rating from visits", String.class).stream()
			.collect(Collectors.groupingBy(r -> r, Collectors.counting()));
		assertThat(ratings).containsEntry("AGAIN", 12L).containsEntry("OKAY", 6L).containsEntry("NOPE", 2L);
		assertThat(jdbc.queryForObject("select count(*) from visits where memo is not null", Integer.class))
			.isEqualTo(10);

		List<Instant> visitedAt = jdbc.queryForList("select visited_at from visits", java.sql.Timestamp.class)
			.stream().map(java.sql.Timestamp::toInstant).toList();
		Instant now = Instant.now();
		assertThat(visitedAt).allSatisfy(t -> assertThat(t).isBefore(now).isAfter(now.minus(Duration.ofDays(182))));
	}

	@Test
	void 시드한_기록이_내_지도에_보인다() throws Exception {
		seed(jihyun, TOKEN).andExpectOk();

		// 광교 일대
		JsonNode pins = data(mockMvc.perform(auth(get("/api/visits/map?swLat=37.25&swLng=127.00&neLat=37.33&neLng=127.10"),
			jihyun)).andExpect(status().isOk()).andReturn());
		assertThat(pins).hasSize(20);
	}

	@Test
	void 다른_사용자에게는_내_시드_기록이_보이지_않는다() throws Exception {
		seed(jihyun, TOKEN).andExpectOk();
		String outsider = jwtProvider.createAccessToken(
			userRepository.save(User.signUpWithKakao(kakaoIdSeq++, "남", null)).getId());

		JsonNode pins = data(mockMvc.perform(auth(get("/api/visits/map?swLat=37.25&swLng=127.00&neLat=37.33&neLng=127.10"),
			outsider)).andExpect(status().isOk()).andReturn());
		assertThat(pins).isEmpty();
	}

	@Test
	void 이미_다른_클럽에_있어도_시드_클럽이_생긴다() throws Exception {
		// 무료 플랜은 클럽 1개. 시드는 이 제한을 적용하지 않는다
		mockMvc.perform(auth(post("/api/clubs"), jihyun).contentType("application/json").content("{\"name\":\"원래클럽\"}"))
			.andExpect(status().isCreated());

		JsonNode seeded = data(seed(jihyun, TOKEN).andExpectOk());

		assertThat(seeded.get("visitCount").asInt()).isEqualTo(20);
		assertThat(jdbc.queryForObject("select count(*) from club_members where user_id = ?", Integer.class, userId))
			.isEqualTo(2);
		// 새 클럽은 원래 클럽과 다른 색
		assertThat(jdbc.queryForList("select distinct color from clubs", String.class)).hasSize(2);
	}

	// ── 멱등 ──

	@Test
	void 여러_번_불러도_중복이_쌓이지_않고_카카오도_다시_부르지_않는다() throws Exception {
		JsonNode first = data(seed(jihyun, TOKEN).andExpectOk());
		int kakaoCalls = KAKAO_REQUESTS.size();

		JsonNode second = data(seed(jihyun, TOKEN).andExpectOk());
		JsonNode third = data(seed(jihyun, TOKEN).andExpectOk());

		assertThat(second.get("created").asBoolean()).isFalse();
		assertThat(second.get("clubId").asLong()).isEqualTo(first.get("clubId").asLong());
		assertThat(third.get("visitCount").asInt()).isEqualTo(20);
		assertThat(KAKAO_REQUESTS).hasSize(kakaoCalls);
		assertCounts(1, 20, 20);
	}

	@Test
	void 동시에_불러도_한_벌만_생긴다() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			List<Callable<Integer>> calls = IntStream.range(0, 4)
				.<Callable<Integer>>mapToObj(i -> () -> seed(jihyun, TOKEN).result.getResponse().getStatus())
				.toList();
			List<Future<Integer>> results = pool.invokeAll(calls);
			for (Future<Integer> result : results) {
				assertThat(result.get()).isEqualTo(200);
			}
		} finally {
			pool.shutdown();
		}
		assertCounts(1, 20, 20);
	}

	@Test
	void 기록을_지운_장소만_다시_채운다() throws Exception {
		seed(jihyun, TOKEN).andExpectOk();
		long removed = jdbc.queryForObject("select min(id) from visits", Long.class);
		mockMvc.perform(auth(delete("/api/visits/" + removed), jihyun)).andExpect(status().isOk());
		// 하나라도 남아 있으면 그대로 둔다
		assertThat(data(seed(jihyun, TOKEN).andExpectOk()).get("visitCount").asInt()).isEqualTo(19);

		// 다 지우면 다시 채운다. 클럽은 새로 만들지 않는다
		jdbc.update("delete from visits");
		JsonNode refilled = data(seed(jihyun, TOKEN).andExpectOk());
		assertThat(refilled.get("created").asBoolean()).isTrue();
		assertCounts(1, 20, 20);
	}

	@Test
	void 사용자마다_자기_시드_클럽을_따로_받는다() throws Exception {
		String mingi = jwtProvider.createAccessToken(
			userRepository.save(User.signUpWithKakao(kakaoIdSeq++, "민기", null)).getId());

		long a = data(seed(jihyun, TOKEN).andExpectOk()).get("clubId").asLong();
		long b = data(seed(mingi, TOKEN).andExpectOk()).get("clubId").asLong();

		assertThat(a).isNotEqualTo(b);
		// 장소는 external_id로 한 행씩만
		assertThat(jdbc.queryForObject("select count(*) from places", Integer.class)).isEqualTo(20);
		assertThat(jdbc.queryForObject("select count(*) from visits", Integer.class)).isEqualTo(40);
	}

	// ── 보안: 토큰이 없거나 틀리면 없는 경로와 똑같은 404 ──

	@Test
	void 토큰_헤더가_없으면_없는_경로와_같은_404() throws Exception {
		Seed noHeader = seed(jihyun, null);

		assertSameAsUnknownPath(noHeader.result, post("/api/dev/nope"));
		assertNothingSeeded();
	}

	@Test
	void 토큰이_틀리면_없는_경로와_같은_404() throws Exception {
		assertSameAsUnknownPath(seed(jihyun, "wrong").result, post("/api/dev/nope"));
		assertSameAsUnknownPath(seed(jihyun, TOKEN.toUpperCase()).result, post("/api/dev/nope"));
		assertSameAsUnknownPath(seed(jihyun, TOKEN + " ").result, post("/api/dev/nope"));
		assertNothingSeeded();
	}

	@Test
	void POST가_아니면_405가_아니라_404() throws Exception {
		MvcResult viaGet = mockMvc.perform(auth(get(DevSeedController.PATH), jihyun)
			.header(DevSeedController.TOKEN_HEADER, TOKEN)).andReturn();
		MvcResult viaPut = mockMvc.perform(auth(put(DevSeedController.PATH), jihyun)
			.header(DevSeedController.TOKEN_HEADER, TOKEN)).andReturn();

		assertSameAsUnknownPath(viaGet, get("/api/dev/nope"));
		assertSameAsUnknownPath(viaPut, put("/api/dev/nope"));
		assertNothingSeeded();
	}

	@Test
	void 로그인하지_않으면_없는_경로와_같은_401() throws Exception {
		MvcResult seed = mockMvc.perform(post(DevSeedController.PATH).header(DevSeedController.TOKEN_HEADER, TOKEN))
			.andReturn();
		MvcResult unknown = mockMvc.perform(post("/api/dev/nope")).andReturn();

		assertThat(seed.getResponse().getStatus()).isEqualTo(401);
		assertThat(seed.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString());
		assertNothingSeeded();
	}

	// ── helpers ──

	private void assertSameAsUnknownPath(MvcResult actual, MockHttpServletRequestBuilder unknownRequest)
		throws Exception {
		MvcResult unknown = mockMvc.perform(auth(unknownRequest, jihyun)).andReturn();
		assertThat(unknown.getResponse().getStatus()).isEqualTo(404);
		assertThat(actual.getResponse().getStatus()).isEqualTo(404);
		assertThat(actual.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString());
	}

	private void assertNothingSeeded() {
		assertCounts(0, 0, 0);
		assertThat(KAKAO_REQUESTS).isEmpty();
	}

	private void assertCounts(int clubs, int places, int visits) {
		assertThat(jdbc.queryForObject("select count(*) from clubs where name = '광교 맛집 (시드)'", Integer.class))
			.isEqualTo(clubs);
		assertThat(jdbc.queryForObject("select count(*) from places", Integer.class)).isEqualTo(places);
		assertThat(jdbc.queryForObject("select count(*) from visits", Integer.class)).isEqualTo(visits);
	}

	private Seed seed(String accessToken, String seedToken) throws Exception {
		MockHttpServletRequestBuilder request = auth(post(DevSeedController.PATH), accessToken);
		if (seedToken != null) {
			request.header(DevSeedController.TOKEN_HEADER, seedToken);
		}
		return new Seed(mockMvc.perform(request).andReturn());
	}

	private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private JsonNode data(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
	}

	private record Seed(MvcResult result) {
		MvcResult andExpectOk() {
			assertThat(result.getResponse().getStatus()).isEqualTo(200);
			return result;
		}
	}

	// ── 카카오 stub ──

	// 광교중앙역 쪽 k-c00~k-c11, 상현역 쪽 k-s00~k-s09와 겹치는 k-c00·k-c01
	private static MockResponse kakaoResponse(RecordedRequest request) {
		double lng = Double.parseDouble(request.getUrl().queryParameter("x"));
		boolean sanghyeon = lng > 127.06;
		List<String> docs = new ArrayList<>();
		if (sanghyeon) {
			docs.add(doc("k-c00", 37.2887, 127.0518));
			docs.add(doc("k-c01", 37.2888, 127.0519));
		}
		for (int i = 0; i < (sanghyeon ? 10 : 12); i++) {
			String id = (sanghyeon ? "k-s" : "k-c") + String.format("%02d", i);
			double lat = (sanghyeon ? 37.2976 : 37.2887) + i * 0.0003;
			double lngOf = (sanghyeon ? 127.0692 : 127.0518) + i * 0.0003;
			docs.add(doc(id, lat, lngOf));
		}
		String body = "{\"documents\": [" + String.join(",", docs) + "]}";
		return new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build();
	}

	// 카카오 표기: x = 경도, y = 위도
	private static String doc(String id, double lat, double lng) {
		return "{\"id\": \"" + id + "\", \"place_name\": \"광교 " + id + "\", "
			+ "\"category_name\": \"음식점 > 한식\", \"category_group_code\": \"FD6\", "
			+ "\"address_name\": \"경기 수원시 영통구\", \"road_address_name\": \"\", "
			+ "\"x\": \"" + lng + "\", \"y\": \"" + lat + "\"}";
	}

	private static MockWebServer startKakaoStub() {
		MockWebServer server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				KAKAO_REQUESTS.add(request);
				return kakaoResponse(request);
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

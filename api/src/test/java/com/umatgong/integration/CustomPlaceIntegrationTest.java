package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.security.JwtProvider;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 「여기 없어요」 클럽 전용 장소. <b>그 클럽 멤버에게만</b> 후보로 보여야 한다 (화면기획서 4.6).
 * 카카오는 항상 빈 결과를 주는 stub이다.
 *
 * <p>지현·민기는 “동네친구들”, 남은 “다른클럽”.
 */
@SpringBootTest(properties = "umatgong.plan.free.max-clubs-per-user=3")
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class CustomPlaceIntegrationTest {

	private static final MockWebServer KAKAO = startKakaoStub();

	// 광교중앙역 근처. 검색 캐시가 테스트끼리 섞이지 않게 테스트마다 다른 좌표·검색어를 쓴다
	private static final double LAT = 37.2887;
	private static final double LNG = 127.0518;

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
	private String jihyun;
	private String mingi;
	private String outsider;
	private long friendsClub;
	private long otherClub;

	@BeforeEach
	void setUp() throws Exception {
		jihyun = newUser("지현");
		mingi = newUser("민기");
		outsider = newUser("남");
		friendsClub = createClub(jihyun, "동네친구들");
		String code = data(perform(post("/api/clubs/" + friendsClub + "/invite"), jihyun, null)).get("inviteCode").asText();
		perform(post("/api/clubs/join"), mingi, "{\"inviteCode\":\"" + code + "\"}").andExpect(status().isOk());
		otherClub = createClub(outsider, "다른클럽");
	}

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	@Test
	void 내_클럽에_가게를_직접_등록하면_좌표가_뒤집히지_않고_클럽_전용으로_저장된다() throws Exception {
		JsonNode place = data(perform(post("/api/places/custom"), jihyun,
			body(friendsClub, "  광교 할머니 국수  ", LAT, LNG)).andExpect(status().isCreated()));

		assertThat(place.get("name").asText()).isEqualTo("광교 할머니 국수");
		assertThat(place.get("clubId").asLong()).isEqualTo(friendsClub);
		assertThat(place.at("/coordinate/lat").asDouble()).isEqualTo(LAT);
		assertThat(place.at("/coordinate/lng").asDouble()).isEqualTo(LNG);
		long id = place.get("id").asLong();
		assertThat(jdbc.queryForObject("select source from places where id = ?", String.class, id)).isEqualTo("USER");
		// PostGIS: ST_X = 경도
		assertThat(jdbc.queryForObject("select ST_X(coordinate::geometry) from places where id = ?", Double.class, id))
			.isEqualTo(LNG);
	}

	@Test
	void 같은_클럽_멤버의_주변_후보에는_보이고_다른_클럽_사용자에게는_안_보인다() throws Exception {
		long id = createCustom(jihyun, friendsClub, "광교 할머니 국수", LAT, LNG);

		JsonNode friend = nearby(mingi, LAT + 0.0005, LNG);
		assertThat(friend).extracting(p -> p.get("id").asLong()).containsExactly(id);
		assertThat(friend.get(0).get("distanceMeters").asInt()).isBetween(40, 70);

		assertThat(nearby(outsider, LAT + 0.0005, LNG)).isEmpty();
	}

	@Test
	void 같은_클럽_멤버의_검색에는_보이고_다른_클럽_사용자에게는_안_보인다() throws Exception {
		long id = createCustom(jihyun, friendsClub, "광교 할머니 국수", LAT, LNG);

		assertThat(search(mingi, "할머니")).extracting(p -> p.get("id").asLong()).containsExactly(id);
		assertThat(search(outsider, "할머니")).isEmpty();
		// LIKE 와일드카드는 글자 그대로다
		assertThat(search(mingi, "%")).isEmpty();
	}

	@Test
	void 멤버가_아닌_클럽에는_등록할_수_없다() throws Exception {
		perform(post("/api/places/custom"), outsider, body(friendsClub, "남의 가게", LAT, LNG))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("CLUB_NOT_FOUND"));
		perform(post("/api/places/custom"), outsider, body(999_999L, "없는 클럽", LAT, LNG))
			.andExpect(status().isNotFound());

		assertThat(jdbc.queryForObject("select count(*) from places", Integer.class)).isZero();
	}

	@Test
	void 입력이_잘못되면_INVALID_INPUT() throws Exception {
		perform(post("/api/places/custom"), jihyun, body(friendsClub, "   ", LAT, LNG))
			.andExpect(status().isBadRequest());
		perform(post("/api/places/custom"), jihyun, body(friendsClub, "가".repeat(101), LAT, LNG))
			.andExpect(status().isBadRequest());
		perform(post("/api/places/custom"), jihyun, body(friendsClub, "국수", 91, LNG))
			.andExpect(status().isBadRequest());
		perform(post("/api/places/custom"), jihyun, "{\"clubId\":" + friendsClub + ",\"name\":\"국수\"}")
			.andExpect(status().isBadRequest());

		assertThat(jdbc.queryForObject("select count(*) from places", Integer.class)).isZero();
	}

	@Test
	void 같은_이름이_50m_안에_있으면_새로_만들지_않는다() throws Exception {
		long first = createCustom(jihyun, friendsClub, "광교 할머니 국수", LAT, LNG);
		long again = createCustom(mingi, friendsClub, "광교 할머니 국수 ", LAT + 0.0002, LNG);
		// 다른 클럽은 따로 만든다 (클럽 경계)
		long other = createCustom(outsider, otherClub, "광교 할머니 국수", LAT, LNG);
		// 멀리 떨어진 같은 이름 가게는 다른 가게다
		long far = createCustom(jihyun, friendsClub, "광교 할머니 국수", LAT + 0.01, LNG);

		assertThat(again).isEqualTo(first);
		assertThat(other).isNotEqualTo(first);
		assertThat(far).isNotEqualTo(first);
		assertThat(jdbc.queryForObject("select count(*) from places", Integer.class)).isEqualTo(3);
	}

	@Test
	void 클럽을_나가면_그_클럽의_전용_장소가_후보에서_빠진다() throws Exception {
		createCustom(jihyun, friendsClub, "광교 할머니 국수", LAT, LNG);
		perform(delete("/api/clubs/" + friendsClub + "/members/me"), mingi, null).andExpect(status().isOk());

		assertThat(nearby(mingi, LAT + 0.0007, LNG)).isEmpty();
		assertThat(search(mingi, "국수")).isEmpty();
	}

	@Test
	void 등록한_장소로_그_클럽에_기록하면_멤버_지도에_핀이_뜬다() throws Exception {
		long id = createCustom(jihyun, friendsClub, "광교 할머니 국수", LAT, LNG);

		perform(post("/api/visits"), jihyun, "{\"placeId\":" + id + ",\"clubId\":" + friendsClub
			+ ",\"rating\":\"AGAIN\",\"visitedAt\":\"2026-09-30T12:00:00Z\"}").andExpect(status().isCreated());

		JsonNode pins = data(perform(get("/api/visits/map?swLat=37.2&swLng=127.0&neLat=37.4&neLng=127.1"), mingi, null)
			.andExpect(status().isOk()));
		assertThat(pins).extracting(p -> p.get("placeId").asLong()).containsExactly(id);
	}

	// ── 도우미 ──

	private String body(long clubId, String name, double lat, double lng) throws Exception {
		var body = objectMapper.createObjectNode();
		body.put("clubId", clubId);
		body.put("name", name);
		body.putObject("coordinate").put("lat", lat).put("lng", lng);
		return objectMapper.writeValueAsString(body);
	}

	private long createCustom(String token, long clubId, String name, double lat, double lng) throws Exception {
		return data(perform(post("/api/places/custom"), token, body(clubId, name, lat, lng))
			.andExpect(status().isCreated())).get("id").asLong();
	}

	private JsonNode nearby(String token, double lat, double lng) throws Exception {
		return data(perform(get("/api/places/nearby?lat=" + lat + "&lng=" + lng + "&radius=300"), token, null)
			.andExpect(status().isOk()));
	}

	private JsonNode search(String token, String query) throws Exception {
		return data(perform(get("/api/places/search").param("query", query), token, null).andExpect(status().isOk()));
	}

	private String newUser(String name) {
		User user = userRepository.save(User.signUpWithKakao(kakaoIdSeq++, name, null));
		return jwtProvider.createAccessToken(user.getId());
	}

	private long createClub(String token, String name) throws Exception {
		return data(perform(post("/api/clubs"), token, "{\"name\":\"" + name + "\"}").andExpect(status().isCreated()))
			.get("id").asLong();
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
		request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(request);
	}

	private JsonNode data(ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
	}

	private static MockWebServer startKakaoStub() {
		MockWebServer server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				return new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
					.body("{\"documents\": []}").build();
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

package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.security.JwtProvider;

/**
 * 스크랩(가고 싶은 곳). 나만 보는 목록이고, 클럽 전용 장소는 그 클럽의 지금 멤버만 다룬다.
 *
 * <p>지현·민기는 “동네친구들”, 남은 “다른클럽”. 카카오 장소 하나, 각 클럽의 전용 장소 하나씩.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class ScrapIntegrationTest {

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
	private long kakaoPlace;
	private long friendsCustom;
	private long otherCustom;

	@BeforeEach
	void setUp() throws Exception {
		jihyun = newUser("지현");
		mingi = newUser("민기");
		outsider = newUser("남");
		friendsClub = createClub(jihyun, "동네친구들");
		String code = data(perform(post("/api/clubs/" + friendsClub + "/invite"), jihyun, null)).get("inviteCode").asText();
		perform(post("/api/clubs/join"), mingi, "{\"inviteCode\":\"" + code + "\"}").andExpect(status().isOk());
		long otherClub = createClub(outsider, "다른클럽");
		kakaoPlace = place("망원동 김반장", null, "k-1");
		friendsCustom = place("광교 할머니 국수", friendsClub, null);
		otherCustom = place("남의 비밀 맛집", otherClub, null);
	}

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, scraps, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	@Test
	void 스크랩하고_해제할_수_있고_여러_번_해도_결과가_같다() throws Exception {
		assertThat(scrapped(jihyun, kakaoPlace)).isFalse();

		perform(put(path(kakaoPlace)), jihyun, null).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.scrapped").value(true));
		perform(put(path(kakaoPlace)), jihyun, null).andExpect(status().isOk());
		assertThat(scrapped(jihyun, kakaoPlace)).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from scraps", Integer.class)).isEqualTo(1);

		perform(delete(path(kakaoPlace)), jihyun, null).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.scrapped").value(false));
		perform(delete(path(kakaoPlace)), jihyun, null).andExpect(status().isOk());
		assertThat(scrapped(jihyun, kakaoPlace)).isFalse();
	}

	@Test
	void 내_스크랩_목록은_최근순이고_남의_스크랩은_보이지_않는다() throws Exception {
		perform(put(path(kakaoPlace)), jihyun, null).andExpect(status().isOk());
		perform(put(path(friendsCustom)), jihyun, null).andExpect(status().isOk());
		jdbc.update("update scraps set created_at = now() - interval '1 day' where place_id = ?", kakaoPlace);
		perform(put(path(kakaoPlace)), mingi, null).andExpect(status().isOk());

		JsonNode mine = data(perform(get("/api/scraps"), jihyun, null).andExpect(status().isOk()));
		assertThat(mine).extracting(s -> s.at("/place/id").asLong()).containsExactly(friendsCustom, kakaoPlace);
		assertThat(mine.get(0).at("/place/clubId").asLong()).isEqualTo(friendsClub);
		assertThat(mine.get(0).has("scrappedAt")).isTrue();

		// 민기의 목록에는 민기 것만, 다른 클럽 사람의 목록은 비어 있다
		assertThat(data(perform(get("/api/scraps"), mingi, null))).extracting(s -> s.at("/place/id").asLong())
			.containsExactly(kakaoPlace);
		assertThat(data(perform(get("/api/scraps"), outsider, null))).isEmpty();
		// 남이 스크랩했다고 내 상태가 바뀌지 않는다
		assertThat(scrapped(outsider, kakaoPlace)).isFalse();
	}

	@Test
	void 다른_클럽의_전용_장소는_없는_장소처럼_다룬다() throws Exception {
		perform(put(path(otherCustom)), jihyun, null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("PLACE_NOT_FOUND"));
		perform(get(path(otherCustom)), jihyun, null).andExpect(status().isNotFound());
		perform(delete(path(otherCustom)), jihyun, null).andExpect(status().isNotFound());
		perform(put(path(999_999L)), jihyun, null).andExpect(status().isNotFound());

		assertThat(jdbc.queryForObject("select count(*) from scraps", Integer.class)).isZero();
	}

	@Test
	void 클럽을_나가면_그_클럽_전용_장소의_스크랩이_목록과_상태에서_빠진다() throws Exception {
		perform(put(path(friendsCustom)), mingi, null).andExpect(status().isOk());
		perform(put(path(kakaoPlace)), mingi, null).andExpect(status().isOk());

		perform(delete("/api/clubs/" + friendsClub + "/members/me"), mingi, null).andExpect(status().isOk());

		assertThat(data(perform(get("/api/scraps"), mingi, null))).extracting(s -> s.at("/place/id").asLong())
			.containsExactly(kakaoPlace);
		perform(get(path(friendsCustom)), mingi, null).andExpect(status().isNotFound());
	}

	@Test
	void 로그인하지_않으면_401() throws Exception {
		mockMvc.perform(put(path(kakaoPlace))).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/scraps")).andExpect(status().isUnauthorized());
	}

	// ── 도우미 ──

	private static String path(long placeId) {
		return "/api/places/" + placeId + "/scrap";
	}

	private boolean scrapped(String token, long placeId) throws Exception {
		return data(perform(get(path(placeId)), token, null).andExpect(status().isOk())).get("scrapped").asBoolean();
	}

	private long place(String name, Long clubId, String externalId) {
		// ST_MakePoint(경도, 위도)
		return jdbc.queryForObject("""
			insert into places (name, coordinate, source, external_id, club_id)
			values (?, ST_SetSRID(ST_MakePoint(127.05, 37.29), 4326)::geography, ?, ?, ?)
			returning id""", Long.class, name, clubId == null ? "API" : "USER", externalId, clubId);
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
}

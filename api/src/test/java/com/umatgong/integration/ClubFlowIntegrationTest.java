package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
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
 * 클럽 API를 HTTP부터 DB까지 흘려 본다. 정원·클럽 수 제한은 테스트하기 쉽게 작은 값으로 바꾼다
 * (값이 설정에서 온다는 것 자체도 확인된다).
 */
@SpringBootTest(properties = {
	"umatgong.plan.free.max-club-members=3",
	"umatgong.plan.free.max-clubs-per-user=2"
})
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class ClubFlowIntegrationTest {

	private static final int MAX_MEMBERS = 3;

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

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	// ── 생성·목록 ──

	@Test
	void 클럽을_만들면_만든_사람이_첫_멤버이자_클럽장이고_색이_자동으로_배정된다() throws Exception {
		String owner = newUser("지현");

		JsonNode club = data(perform(post("/api/clubs"), owner, "{\"name\":\"  동네친구들  \"}")
			.andExpect(status().isCreated()));

		assertThat(club.get("name").asText()).isEqualTo("동네친구들");
		assertThat(club.get("color").asText()).isEqualTo("SAGE");
		assertThat(club.get("memberCount").asLong()).isEqualTo(1);
		assertThat(club.get("maxMembers").asInt()).isEqualTo(MAX_MEMBERS);
		assertThat(club.get("owner").asBoolean()).isTrue();
		assertThat(club.get("full").asBoolean()).isFalse();
		// 초대 코드는 목록·생성 응답에 섞여 나가지 않는다 (초대 API에서만)
		assertThat(club.has("inviteCode")).isFalse();

		JsonNode mine = data(perform(get("/api/clubs"), owner, null).andExpect(status().isOk()));
		assertThat(mine).hasSize(1);
		assertThat(mine.get(0).get("id").asLong()).isEqualTo(club.get("id").asLong());
	}

	@Test
	void 두_번째_클럽은_첫_클럽과_다른_색이다() throws Exception {
		String owner = newUser("지현");
		createClub(owner, "동네친구들");

		JsonNode second = data(perform(post("/api/clubs"), owner, "{\"name\":\"회사팀\"}").andExpect(status().isCreated()));

		assertThat(second.get("color").asText()).isEqualTo("SKY");
	}

	@Test
	void 클럽_수_제한을_넘으면_만들지도_합류하지도_못한다() throws Exception {
		String user = newUser("지현");
		createClub(user, "하나");
		createClub(user, "둘");

		perform(post("/api/clubs"), user, "{\"name\":\"셋\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CLUB_LIMIT_REACHED"));

		String other = newUser("민기");
		long otherClub = createClub(other, "남의 클럽");
		perform(post("/api/clubs/join"), user, joinBody(inviteCode(other, otherClub)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CLUB_LIMIT_REACHED"));
	}

	@Test
	void 이름이_비었거나_30자를_넘으면_INVALID_INPUT() throws Exception {
		String user = newUser("지현");

		perform(post("/api/clubs"), user, "{\"name\":\"   \"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		perform(post("/api/clubs"), user, "{\"name\":\"" + "가".repeat(31) + "\"}").andExpect(status().isBadRequest());
	}

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(get("/api/clubs")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/clubs/join").contentType(MediaType.APPLICATION_JSON).content(joinBody("x")))
			.andExpect(status().isUnauthorized());
	}

	// ── 초대·합류 ──

	@Test
	void 초대_코드로_합류하면_멤버가_된다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String friend = newUser("민기");

		JsonNode joined = data(perform(post("/api/clubs/join"), friend, joinBody(inviteCode(owner, clubId)))
			.andExpect(status().isOk()));

		assertThat(joined.get("id").asLong()).isEqualTo(clubId);
		assertThat(joined.get("memberCount").asLong()).isEqualTo(2);
		assertThat(joined.get("owner").asBoolean()).isFalse();
		JsonNode members = data(perform(get("/api/clubs/" + clubId + "/members"), friend, null).andExpect(status().isOk()));
		assertThat(members).extracting(m -> m.get("name").asText()).containsExactly("지현", "민기");
		assertThat(members.get(0).get("owner").asBoolean()).isTrue();
		assertThat(members.get(1).get("owner").asBoolean()).isFalse();
	}

	@Test
	void 초대_코드는_추측할_수_없는_32자이고_재발급하면_이전_코드는_무효다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");

		String first = inviteCode(owner, clubId);
		assertThat(first).hasSize(32).matches("[A-Za-z0-9_-]{32}");
		// 재발급하지 않으면 같은 코드다 (이미 보낸 링크가 계속 통한다)
		assertThat(inviteCode(owner, clubId)).isEqualTo(first);

		String second = data(perform(post("/api/clubs/" + clubId + "/invite"), owner, "{\"reissue\":true}")
			.andExpect(status().isOk())).get("inviteCode").asText();
		assertThat(second).isNotEqualTo(first);

		perform(post("/api/clubs/join"), newUser("민기"), joinBody(first))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("INVALID_INVITE_CODE"));
		perform(post("/api/clubs/join"), newUser("세영"), joinBody(second)).andExpect(status().isOk());
	}

	@Test
	void 초대_코드_재발급은_클럽장만_한다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String friend = newUser("민기");
		join(friend, inviteCode(owner, clubId));

		// 멤버는 지금 코드를 받아 공유할 수는 있다
		perform(post("/api/clubs/" + clubId + "/invite"), friend, null).andExpect(status().isOk());
		perform(post("/api/clubs/" + clubId + "/invite"), friend, "{\"reissue\":true}")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void 유효하지_않은_초대_코드는_거부한다() throws Exception {
		String user = newUser("지현");

		for (String code : List.of("not-a-real-code", "1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
			perform(post("/api/clubs/join"), user, joinBody(code))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("INVALID_INVITE_CODE"));
		}
		perform(post("/api/clubs/join"), user, joinBody("")).andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("select count(*) from club_members", Integer.class)).isZero();
	}

	@Test
	void 이미_속한_클럽에_다시_합류하면_ALREADY_CLUB_MEMBER이고_멤버는_늘지_않는다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String code = inviteCode(owner, clubId);
		String friend = newUser("민기");
		join(friend, code);

		perform(post("/api/clubs/join"), friend, joinBody(code))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("ALREADY_CLUB_MEMBER"));
		perform(post("/api/clubs/join"), owner, joinBody(code))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("ALREADY_CLUB_MEMBER"));
		assertThat(jdbc.queryForObject("select count(*) from club_members where club_id = ?", Integer.class, clubId))
			.isEqualTo(2);
	}

	// ── 정원 ──

	@Test
	void 정원이_차면_초대와_합류만_막고_기존_멤버는_그대로_쓴다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String code = inviteCode(owner, clubId);
		String second = newUser("민기");
		String third = newUser("세영");
		join(second, code);
		join(third, code);

		// 초대 차단
		perform(post("/api/clubs/" + clubId + "/invite"), owner, null)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CLUB_FULL"));
		// 이미 받은 코드로 합류도 차단
		perform(post("/api/clubs/join"), newUser("늦은친구"), joinBody(code))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("CLUB_FULL"));

		// 기존 멤버는 모두 정상 조회
		for (String member : List.of(owner, second, third)) {
			JsonNode clubs = data(perform(get("/api/clubs"), member, null).andExpect(status().isOk()));
			assertThat(clubs.get(0).get("full").asBoolean()).isTrue();
			assertThat(clubs.get(0).get("memberCount").asLong()).isEqualTo(MAX_MEMBERS);
			perform(get("/api/clubs/" + clubId + "/members"), member, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(MAX_MEMBERS));
		}
	}

	@Test
	void 마지막_한_자리에_동시에_합류해도_정원을_넘지_않는다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String code = inviteCode(owner, clubId);
		join(newUser("민기"), code);
		// 남은 자리 1개에 6명이 동시에 들어온다
		List<String> latecomers = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			latecomers.add(newUser("친구" + i));
		}

		ExecutorService pool = Executors.newFixedThreadPool(latecomers.size());
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		for (String token : latecomers) {
			Callable<Integer> task = () -> {
				start.await();
				return perform(post("/api/clubs/join"), token, joinBody(code)).andReturn().getResponse().getStatus();
			};
			results.add(pool.submit(task));
		}
		start.countDown();
		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get(20, TimeUnit.SECONDS));
		}
		pool.shutdown();

		assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
		assertThat(statuses).filteredOn(s -> s == 409).hasSize(latecomers.size() - 1);
		assertThat(jdbc.queryForObject("select count(*) from club_members where club_id = ?", Integer.class, clubId))
			.isEqualTo(MAX_MEMBERS);
	}

	// ── 클럽 경계 ──

	@Test
	void 다른_클럽_멤버는_이_클럽의_멤버_초대_탈퇴_어느_것도_할_수_없고_존재도_알_수_없다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String outsider = newUser("남");
		createClub(outsider, "다른클럽");

		perform(get("/api/clubs/" + clubId + "/members"), outsider, null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("CLUB_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());
		perform(post("/api/clubs/" + clubId + "/invite"), outsider, null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("CLUB_NOT_FOUND"))
			.andExpect(jsonPath("$.data").doesNotExist());
		perform(delete("/api/clubs/" + clubId + "/members/me"), outsider, null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("CLUB_NOT_FOUND"));
		// 내 클럽 목록에도 남의 클럽은 없다
		JsonNode outsiderClubs = data(perform(get("/api/clubs"), outsider, null).andExpect(status().isOk()));
		assertThat(outsiderClubs).extracting(c -> c.get("id").asLong()).doesNotContain(clubId);
		// 없는 클럽과 남의 클럽은 응답이 같다 (있다는 사실도 드러나지 않는다)
		String missing = perform(get("/api/clubs/999999/members"), outsider, null).andReturn().getResponse()
			.getContentAsString();
		String others = perform(get("/api/clubs/" + clubId + "/members"), outsider, null).andReturn().getResponse()
			.getContentAsString();
		assertThat(others).isEqualTo(missing);
		assertThat(jdbc.queryForObject("select count(*) from club_members where club_id = ?", Integer.class, clubId))
			.isEqualTo(1);
	}

	// ── 탈퇴 ──

	@Test
	void 탈퇴하면_더_이상_클럽을_볼_수_없다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String friend = newUser("민기");
		join(friend, inviteCode(owner, clubId));

		perform(delete("/api/clubs/" + clubId + "/members/me"), friend, null).andExpect(status().isOk());

		perform(get("/api/clubs/" + clubId + "/members"), friend, null).andExpect(status().isNotFound());
		assertThat(data(perform(get("/api/clubs"), friend, null))).isEmpty();
		perform(get("/api/clubs/" + clubId + "/members"), owner, null)
			.andExpect(jsonPath("$.data.length()").value(1));
	}

	@Test
	void 클럽장이_탈퇴해도_클럽과_남은_멤버는_그대로다() throws Exception {
		String owner = newUser("지현");
		long clubId = createClub(owner, "동네친구들");
		String friend = newUser("민기");
		join(friend, inviteCode(owner, clubId));

		perform(delete("/api/clubs/" + clubId + "/members/me"), owner, null).andExpect(status().isOk());

		JsonNode members = data(perform(get("/api/clubs/" + clubId + "/members"), friend, null).andExpect(status().isOk()));
		assertThat(members).hasSize(1);
		assertThat(members.get(0).get("owner").asBoolean()).isFalse();
		assertThat(jdbc.queryForObject("select created_by from clubs where id = ?", Long.class, clubId)).isNull();
		// 클럽장이 없으면 재발급은 아무도 못 하지만 지금 코드로 초대는 계속된다
		perform(post("/api/clubs/" + clubId + "/invite"), friend, null).andExpect(status().isOk());
	}

	@Test
	void 탈퇴하면_클럽_수_제한에서_빠져_다른_클럽에_들어갈_수_있다() throws Exception {
		String user = newUser("지현");
		long a = createClub(user, "하나");
		createClub(user, "둘");
		String other = newUser("민기");
		long otherClub = createClub(other, "셋");
		String code = inviteCode(other, otherClub);

		perform(post("/api/clubs/join"), user, joinBody(code)).andExpect(status().isConflict());
		perform(delete("/api/clubs/" + a + "/members/me"), user, null).andExpect(status().isOk());
		perform(post("/api/clubs/join"), user, joinBody(code)).andExpect(status().isOk());
	}

	// ── 도우미 ──

	private String newUser(String name) {
		User user = userRepository.save(User.signUpWithKakao(kakaoIdSeq++, name, null));
		return jwtProvider.createAccessToken(user.getId());
	}

	private long createClub(String token, String name) throws Exception {
		return data(perform(post("/api/clubs"), token, "{\"name\":\"" + name + "\"}").andExpect(status().isCreated()))
			.get("id").asLong();
	}

	private String inviteCode(String token, long clubId) throws Exception {
		return data(perform(post("/api/clubs/" + clubId + "/invite"), token, null).andExpect(status().isOk()))
			.get("inviteCode").asText();
	}

	private void join(String token, String code) throws Exception {
		perform(post("/api/clubs/join"), token, joinBody(code)).andExpect(status().isOk());
	}

	private static String joinBody(String code) {
		return "{\"inviteCode\":\"" + code + "\"}";
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

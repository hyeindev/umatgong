package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

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
 * 기록 API를 HTTP부터 DB(PostGIS)까지 흘려 본다. 이 단계의 핵심인 클럽 경계·비공개·작성자 권한을 확인한다.
 *
 * <p>등장인물: 지현·민기는 “동네친구들” 클럽, 남은 “다른클럽”에만 속한다.
 */
// 사진 저장소 주소는 접속되지 않는 곳으로 둔다. 기록에 붙는 주소의 소유 검사만 쓰고, 지울 때의 저장소 호출은 실패해도 된다
@SpringBootTest(properties = {
	"umatgong.plan.free.max-clubs-per-user=3",
	"umatgong.photo.supabase-url=" + VisitFlowIntegrationTest.STORAGE,
	"umatgong.photo.service-key=test-service-key"
})
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class VisitFlowIntegrationTest {

	static final String STORAGE = "http://localhost:1";

	// 망원동, 망원동에서 약 300m, 성수동(망원에서 약 12km), 부산
	private static final double[] MANGWON = {37.5563, 126.9236};
	private static final double[] MANGWON_NEAR = {37.5590, 126.9230};
	private static final double[] SEONGSU = {37.5445, 127.0560};
	private static final double[] BUSAN = {35.1587, 129.1604};

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
	private long mangwon;
	private long mangwonNear;
	private long seongsu;
	private long busan;

	@BeforeEach
	void setUp() throws Exception {
		jihyun = newUser("지현");
		mingi = newUser("민기");
		outsider = newUser("남");
		friendsClub = createClub(jihyun, "동네친구들");
		join(mingi, inviteCode(jihyun, friendsClub));
		otherClub = createClub(outsider, "다른클럽");
		mangwon = place("망원동 김반장", MANGWON);
		mangwonNear = place("망원 손칼국수", MANGWON_NEAR);
		seongsu = place("성수 우육면관", SEONGSU);
		busan = place("해운대 국밥", BUSAN);
	}

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	// ── 생성 ──

	@Test
	void 내_클럽에_기록하면_같은_클럽_멤버의_지도에_핀으로_보인다() throws Exception {
		String thumb = thumbOf("지현", "t1.jpg");
		long visitId = record(jihyun, mangwon, friendsClub, "AGAIN", "막창 최고", thumb);

		JsonNode pins = pins(mingi, SEOUL_BOX);
		assertThat(pins).hasSize(1);
		JsonNode pin = pins.get(0);
		assertThat(pin.get("id").asLong()).isEqualTo(visitId);
		assertThat(pin.get("placeId").asLong()).isEqualTo(mangwon);
		assertThat(pin.at("/coordinate/lat").asDouble()).isEqualTo(MANGWON[0]);
		assertThat(pin.at("/coordinate/lng").asDouble()).isEqualTo(MANGWON[1]);
		assertThat(pin.get("clubColor").asText()).isEqualTo("SAGE");
		assertThat(pin.get("rating").asText()).isEqualTo("AGAIN");
		assertThat(pin.get("thumbnailUrl").asText()).isEqualTo(thumb);
		// 핀은 가볍게: 메모·작성자 같은 상세는 없다
		assertThat(pin.has("memo")).isFalse();
		assertThat(pin.has("author")).isFalse();
	}

	@Test
	void 생성_응답에_기록_상세가_온다() throws Exception {
		JsonNode created = data(perform(post("/api/visits"), jihyun, visitBody(mangwon, friendsClub, "OKAY",
			"  국물이 좋음  ", null, null)).andExpect(status().isCreated()));

		assertThat(created.at("/place/name").asText()).isEqualTo("망원동 김반장");
		assertThat(created.at("/club/id").asLong()).isEqualTo(friendsClub);
		assertThat(created.at("/club/color").asText()).isEqualTo("SAGE");
		assertThat(created.at("/author/name").asText()).isEqualTo("지현");
		assertThat(created.get("memo").asText()).isEqualTo("국물이 좋음");
		assertThat(created.get("visibility").asText()).isEqualTo("CLUB");
		assertThat(created.get("mine").asBoolean()).isTrue();
	}

	@Test
	void 멤버가_아닌_클럽에는_기록할_수_없다() throws Exception {
		perform(post("/api/visits"), outsider, visitBody(mangwon, friendsClub, "AGAIN", null, null, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("CLUB_NOT_FOUND"));
		perform(post("/api/visits"), outsider, visitBody(mangwon, 999_999L, "AGAIN", null, null, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("CLUB_NOT_FOUND"));

		assertThat(jdbc.queryForObject("select count(*) from visits", Integer.class)).isZero();
	}

	@Test
	void 입력이_잘못되면_INVALID_INPUT() throws Exception {
		perform(post("/api/visits"), jihyun, "{\"placeId\":" + mangwon + ",\"rating\":\"FIVE_STARS\","
			+ "\"visitedAt\":\"2026-09-30T12:00:00Z\"}").andExpect(status().isBadRequest());
		perform(post("/api/visits"), jihyun, visitBody(mangwon, friendsClub, "AGAIN", "가".repeat(201), null, null))
			.andExpect(status().isBadRequest());
		perform(post("/api/visits"), jihyun, visitBody(mangwon, friendsClub, "AGAIN", null, "https://evil.test/a.jpg", null))
			.andExpect(status().isBadRequest());
		perform(post("/api/visits"), jihyun, "{\"placeId\":" + mangwon + ",\"rating\":\"AGAIN\","
			+ "\"visitedAt\":\"2099-01-01T00:00:00Z\"}").andExpect(status().isBadRequest());
		perform(post("/api/visits"), jihyun, visitBody(mangwon, null, "AGAIN", null, null, "CLUB"))
			.andExpect(status().isBadRequest());
		perform(post("/api/visits"), jihyun, visitBody(999_999L, friendsClub, "AGAIN", null, null, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("PLACE_NOT_FOUND"));
		assertThat(jdbc.queryForObject("select count(*) from visits", Integer.class)).isZero();
	}

	// ── 클럽 경계 ──

	@Test
	void 다른_클럽_사용자의_지도에는_이_클럽_기록이_나오지_않는다() throws Exception {
		record(jihyun, mangwon, friendsClub, "AGAIN", null, null);
		long outsiderVisit = record(outsider, mangwon, otherClub, "OKAY", null, null);

		JsonNode outsiderPins = pins(outsider, SEOUL_BOX);
		assertThat(outsiderPins).extracting(p -> p.get("id").asLong()).containsExactly(outsiderVisit);
		// 반대로도 마찬가지
		assertThat(pins(mingi, SEOUL_BOX)).extracting(p -> p.get("id").asLong()).doesNotContain(outsiderVisit);
		// clubIds로 남의 클럽을 찍어 보내도 나오지 않는다
		JsonNode forced = data(perform(get("/api/visits/map" + SEOUL_BOX + "&clubIds=" + friendsClub), outsider, null)
			.andExpect(status().isOk()));
		assertThat(forced).isEmpty();
	}

	@Test
	void 다른_클럽_사용자가_장소의_기록을_조회하면_이_클럽_기록이_나오지_않는다() throws Exception {
		record(jihyun, mangwon, friendsClub, "AGAIN", "막창 최고", null);

		JsonNode outsiderView = data(perform(get("/api/places/" + mangwon + "/visits"), outsider, null)
			.andExpect(status().isOk()));
		assertThat(outsiderView).isEmpty();

		JsonNode friendView = data(perform(get("/api/places/" + mangwon + "/visits"), mingi, null)
			.andExpect(status().isOk()));
		assertThat(friendView).hasSize(1);
		assertThat(friendView.get(0).get("memo").asText()).isEqualTo("막창 최고");
		assertThat(friendView.get(0).get("mine").asBoolean()).isFalse();
	}

	@Test
	void 다른_클럽_사용자의_주변_목록에도_나오지_않는다() throws Exception {
		record(jihyun, mangwon, friendsClub, "AGAIN", null, null);

		assertThat(nearby(outsider, MANGWON, 1000, null)).isEmpty();
		assertThat(nearby(mingi, MANGWON, 1000, null)).hasSize(1);
	}

	@Test
	void 다른_클럽_사용자는_기록을_수정_삭제할_수_없고_있는지도_모른다() throws Exception {
		long visitId = record(jihyun, mangwon, friendsClub, "AGAIN", "원래 메모", null);

		perform(patch("/api/visits/" + visitId), outsider, "{\"rating\":\"NOPE\"}")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
		perform(delete("/api/visits/" + visitId), outsider, null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
		assertUnchanged(visitId, "AGAIN", "원래 메모");
	}

	// ── 비공개 ──

	@Test
	void PRIVATE_기록은_같은_클럽_멤버에게도_어디서도_보이지_않는다() throws Exception {
		long privateVisit = data(perform(post("/api/visits"), jihyun,
			visitBody(mangwon, friendsClub, "NOPE", "나만 아는 곳", null, "PRIVATE")).andExpect(status().isCreated()))
			.get("id").asLong();

		assertThat(pins(mingi, SEOUL_BOX)).isEmpty();
		assertThat(nearby(mingi, MANGWON, 1000, null)).isEmpty();
		assertThat(data(perform(get("/api/places/" + mangwon + "/visits"), mingi, null))).isEmpty();
		perform(patch("/api/visits/" + privateVisit), mingi, "{\"memo\":\"x\"}").andExpect(status().isNotFound());
		perform(delete("/api/visits/" + privateVisit), mingi, null).andExpect(status().isNotFound());

		// 작성자 본인에게는 보인다
		assertThat(pins(jihyun, SEOUL_BOX)).extracting(p -> p.get("id").asLong()).containsExactly(privateVisit);
		assertThat(data(perform(get("/api/places/" + mangwon + "/visits"), jihyun, null))).hasSize(1);
		assertThat(data(perform(get("/api/visits/me"), jihyun, null)).get("items")).hasSize(1);
	}

	@Test
	void 클럽_없이_남긴_기록은_나만_본다() throws Exception {
		JsonNode solo = data(perform(post("/api/visits"), outsider, visitBody(mangwon, null, "AGAIN", null, null, null))
			.andExpect(status().isCreated()));

		assertThat(solo.get("visibility").asText()).isEqualTo("PRIVATE");
		assertThat(solo.get("club").isNull()).isTrue();
		assertThat(pins(outsider, SEOUL_BOX)).hasSize(1);
		assertThat(pins(outsider, SEOUL_BOX).get(0).get("clubColor").isNull()).isTrue();
		assertThat(pins(jihyun, SEOUL_BOX)).isEmpty();
		assertThat(pins(mingi, SEOUL_BOX)).isEmpty();
	}

	// ── 작성자 권한 ──

	@Test
	void 같은_클럽_멤버라도_남의_기록은_수정_삭제할_수_없다() throws Exception {
		long visitId = record(jihyun, mangwon, friendsClub, "AGAIN", "원래 메모", null);

		perform(patch("/api/visits/" + visitId), mingi, "{\"rating\":\"NOPE\",\"memo\":\"바꿈\"}")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
		perform(delete("/api/visits/" + visitId), mingi, null)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
		assertUnchanged(visitId, "AGAIN", "원래 메모");
	}

	@Test
	void 내_기록은_평가와_메모를_고치고_지울_수_있다() throws Exception {
		long visitId = record(jihyun, mangwon, friendsClub, "AGAIN", "원래 메모", thumbOf("지현", "t.jpg"));

		JsonNode updated = data(perform(patch("/api/visits/" + visitId), jihyun, "{\"rating\":\"OKAY\"}")
			.andExpect(status().isOk()));
		assertThat(updated.get("rating").asText()).isEqualTo("OKAY");
		assertThat(updated.get("memo").asText()).isEqualTo("원래 메모");
		JsonNode cleared = data(perform(patch("/api/visits/" + visitId), jihyun, "{\"memo\":\"\"}")
			.andExpect(status().isOk()));
		assertThat(cleared.get("memo").isNull()).isTrue();

		perform(delete("/api/visits/" + visitId), jihyun, null).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("select count(*) from visits", Integer.class)).isZero();
		// 썸네일도 함께 지워진다
		assertThat(jdbc.queryForObject("select count(*) from visit_photos", Integer.class)).isZero();
	}

	// ── 탈퇴 ──

	@Test
	void 클럽을_탈퇴하면_그_클럽_기록이_더_이상_보이지_않는다() throws Exception {
		record(jihyun, mangwon, friendsClub, "AGAIN", null, null);
		long mingiOwn = record(mingi, mangwonNear, friendsClub, "OKAY", null, null);
		assertThat(pins(mingi, SEOUL_BOX)).hasSize(2);

		perform(delete("/api/clubs/" + friendsClub + "/members/me"), mingi, null).andExpect(status().isOk());

		assertThat(pins(mingi, SEOUL_BOX)).isEmpty();
		assertThat(nearby(mingi, MANGWON, 1000, null)).isEmpty();
		assertThat(data(perform(get("/api/places/" + mangwon + "/visits"), mingi, null))).isEmpty();
		// 탈퇴한 클럽에 남긴 내 기록도 내보내지 않는다 (속하지 않은 클럽의 기록)
		assertThat(data(perform(get("/api/visits/me"), mingi, null)).get("items")).isEmpty();
		perform(patch("/api/visits/" + mingiOwn), mingi, "{\"rating\":\"AGAIN\"}").andExpect(status().isNotFound());
		// 다만 내 기록을 지우는 것은 된다 (기획서 C-04)
		perform(delete("/api/visits/" + mingiOwn), mingi, null).andExpect(status().isOk());
		// 남은 멤버에게는 그대로 보인다
		assertThat(pins(jihyun, SEOUL_BOX)).hasSize(1);
	}

	// ── 공간 조회 ──

	@Test
	void 지도_영역_밖의_기록은_나오지_않는다() throws Exception {
		long inside = record(jihyun, mangwon, friendsClub, "AGAIN", null, null);
		record(jihyun, busan, friendsClub, "AGAIN", null, null);

		// 망원동 주변만 보이는 작은 영역
		String mangwonBox = "?swLat=37.55&swLng=126.91&neLat=37.57&neLng=126.93";
		assertThat(pins(mingi, mangwonBox)).extracting(p -> p.get("id").asLong()).containsExactly(inside);
		// 전국 영역이면 둘 다
		assertThat(pins(mingi, "?swLat=33.0&swLng=124.5&neLat=38.7&neLng=131.0")).hasSize(2);
		perform(get("/api/visits/map?swLat=38&swLng=126&neLat=37&neLng=127"), mingi, null)
			.andExpect(status().isBadRequest());
	}

	@Test
	void 주변_조회는_반경_안만_가까운_순으로_주고_평가로_거를_수_있다() throws Exception {
		record(jihyun, mangwonNear, friendsClub, "OKAY", null, null);
		record(jihyun, mangwon, friendsClub, "AGAIN", null, null);
		record(jihyun, seongsu, friendsClub, "AGAIN", null, null);

		JsonNode near = nearby(mingi, MANGWON, 1000, null);
		assertThat(near).extracting(v -> v.at("/place/id").asLong()).containsExactly(mangwon, mangwonNear);
		assertThat(near.get(0).get("distanceMeters").asInt()).isZero();
		assertThat(near.get(1).get("distanceMeters").asInt()).isBetween(250, 350);

		assertThat(nearby(mingi, MANGWON, 1000, "AGAIN")).extracting(v -> v.at("/place/id").asLong())
			.containsExactly(mangwon);
		assertThat(nearby(mingi, MANGWON, 20_000, null)).hasSize(3);
	}

	// ── 내 기록 ──

	@Test
	void 내_기록은_최근_방문순으로_커서를_따라_빠짐없이_겹치지_않게_넘긴다() throws Exception {
		List<Long> ids = new ArrayList<>();
		for (int day = 1; day <= 5; day++) {
			ids.add(data(perform(post("/api/visits"), jihyun, "{\"placeId\":" + mangwon + ",\"clubId\":" + friendsClub
				+ ",\"rating\":\"AGAIN\",\"visitedAt\":\"2026-09-0" + day + "T12:00:00Z\"}")
				.andExpect(status().isCreated())).get("id").asLong());
		}
		record(mingi, mangwon, friendsClub, "OKAY", null, null); // 남의 기록은 섞이지 않는다

		List<Long> seen = new ArrayList<>();
		String cursor = null;
		int pages = 0;
		do {
			JsonNode page = data(perform(get("/api/visits/me?size=2" + (cursor == null ? "" : "&cursor=" + cursor)),
				jihyun, null).andExpect(status().isOk()));
			page.get("items").forEach(v -> seen.add(v.get("id").asLong()));
			cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
			pages++;
		} while (cursor != null);

		assertThat(pages).isEqualTo(3);
		assertThat(seen).containsExactly(ids.get(4), ids.get(3), ids.get(2), ids.get(1), ids.get(0));
		perform(get("/api/visits/me?cursor=garbage"), jihyun, null).andExpect(status().isBadRequest());
	}

	// ── 커스텀 장소 ──

	@Test
	void 다른_클럽의_커스텀_장소는_없는_장소와_같다() throws Exception {
		long custom = jdbc.queryForObject("""
			insert into places (name, coordinate, source, club_id)
			values ('이름 없는 포차', ST_SetSRID(ST_MakePoint(126.92, 37.556), 4326)::geography, 'USER', ?)
			returning id""", Long.class, friendsClub);

		perform(get("/api/places/" + custom + "/visits"), outsider, null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("PLACE_NOT_FOUND"));
		perform(post("/api/visits"), outsider, visitBody(custom, otherClub, "AGAIN", null, null, null))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("PLACE_NOT_FOUND"));
		// 내 클럽 커스텀 장소는 기록할 수 있다
		perform(post("/api/visits"), mingi, visitBody(custom, friendsClub, "AGAIN", null, null, null))
			.andExpect(status().isCreated());
		perform(get("/api/places/" + custom + "/visits"), mingi, null).andExpect(status().isOk());
	}

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(get("/api/visits/map" + SEOUL_BOX)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/places/" + mangwon + "/visits")).andExpect(status().isUnauthorized());
	}

	// ── 사진 ──

	@Test
	void 내가_올린_썸네일_3장까지_올린_순서대로_붙는다() throws Exception {
		List<String> thumbs = List.of(thumbOf("지현", "a.jpg"), thumbOf("지현", "b.jpg"), thumbOf("지현", "c.jpg"));
		var body = objectMapper.readTree(visitBody(mangwon, friendsClub, "AGAIN", null, null, null));
		var array = ((com.fasterxml.jackson.databind.node.ObjectNode) body).putArray("thumbnailUrls");
		thumbs.forEach(array::add);

		JsonNode created = data(perform(post("/api/visits"), jihyun, body.toString()).andExpect(status().isCreated()));

		assertThat(created.get("thumbnailUrl").asText()).isEqualTo(thumbs.get(0));
		assertThat(created.get("thumbnailUrls")).extracting(JsonNode::asText).containsExactlyElementsOf(thumbs);
	}

	@Test
	void 남이_올린_사진이나_4장_이상은_붙일_수_없다() throws Exception {
		// 민기가 올린 사진을 지현이 붙이려 한다
		perform(post("/api/visits"), jihyun, visitBody(mangwon, friendsClub, "AGAIN", null, thumbOf("민기", "m.jpg"), null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		// 경로를 넘어가는 주소
		perform(post("/api/visits"), jihyun,
			visitBody(mangwon, friendsClub, "AGAIN", null, thumbOf("지현", "../2/m.jpg"), null))
			.andExpect(status().isBadRequest());

		var body = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(
			visitBody(mangwon, friendsClub, "AGAIN", null, null, null));
		var array = body.putArray("thumbnailUrls");
		for (int i = 0; i < 4; i++) {
			array.add(thumbOf("지현", i + ".jpg"));
		}
		perform(post("/api/visits"), jihyun, body.toString()).andExpect(status().isBadRequest());

		assertThat(jdbc.queryForObject("select count(*) from visits", Integer.class)).isZero();
	}

	// ── 도우미 ──

	/** 이 사용자가 올렸을 때 저장소가 주는 썸네일 주소 */
	private String thumbOf(String userName, String file) {
		long userId = jdbc.queryForObject("select id from users where name = ?", Long.class, userName);
		return STORAGE + "/storage/v1/object/public/thumbnails/thumbs/" + userId + "/" + file;
	}

	private static final String SEOUL_BOX = "?swLat=37.40&swLng=126.75&neLat=37.70&neLng=127.20";

	private long record(String token, long placeId, Long clubId, String rating, String memo, String thumb)
		throws Exception {
		return data(perform(post("/api/visits"), token, visitBody(placeId, clubId, rating, memo, thumb, null))
			.andExpect(status().isCreated())).get("id").asLong();
	}

	private String visitBody(long placeId, Long clubId, String rating, String memo, String thumb, String visibility)
		throws Exception {
		var body = objectMapper.createObjectNode();
		body.put("placeId", placeId);
		if (clubId != null) {
			body.put("clubId", clubId);
		}
		body.put("rating", rating);
		body.put("visitedAt", "2026-09-30T12:00:00Z");
		if (memo != null) {
			body.put("memo", memo);
		}
		if (thumb != null) {
			body.putArray("thumbnailUrls").add(thumb);
		}
		if (visibility != null) {
			body.put("visibility", visibility);
		}
		return objectMapper.writeValueAsString(body);
	}

	private JsonNode pins(String token, String box) throws Exception {
		return data(perform(get("/api/visits/map" + box), token, null).andExpect(status().isOk()));
	}

	private JsonNode nearby(String token, double[] center, int radius, String rating) throws Exception {
		String query = "?lat=" + center[0] + "&lng=" + center[1] + "&radius=" + radius
			+ (rating == null ? "" : "&rating=" + rating);
		return data(perform(get("/api/visits/nearby" + query), token, null).andExpect(status().isOk()));
	}

	private void assertUnchanged(long visitId, String rating, String memo) {
		assertThat(jdbc.queryForObject("select rating from visits where id = ?", String.class, visitId)).isEqualTo(rating);
		assertThat(jdbc.queryForObject("select memo from visits where id = ?", String.class, visitId)).isEqualTo(memo);
	}

	private long place(String name, double[] latLng) {
		// ST_MakePoint(경도, 위도)
		return jdbc.queryForObject("""
			insert into places (name, coordinate, category, source, external_id)
			values (?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, '음식점 > 한식', 'API', ?)
			returning id""", Long.class, name, latLng[1], latLng[0], "kakao-" + name);
	}

	private String newUser(String name) {
		User user = userRepository.save(User.signUpWithKakao(kakaoIdSeq++, name, null));
		return jwtProvider.createAccessToken(user.getId());
	}

	private long createClub(String token, String name) throws Exception {
		return data(perform(post("/api/clubs"), token, "{\"name\":\"" + name + "\"}").andExpect(status().isCreated()))
			.get("id").asLong();
	}

	private String inviteCode(String token, long clubId) throws Exception {
		return data(perform(post("/api/clubs/" + clubId + "/invite"), token, null)).get("inviteCode").asText();
	}

	private void join(String token, String code) throws Exception {
		perform(post("/api/clubs/join"), token, "{\"inviteCode\":\"" + code + "\"}").andExpect(status().isOk());
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

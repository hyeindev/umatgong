package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.global.kakao.KakaoProperties;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 로그인 → 갱신 → 로그아웃을 HTTP부터 DB까지 실제로 흘려 본다. 카카오만 MockWebServer로 대신한다.
 *
 * <p>트랜잭션으로 감싸지 않는다. 재사용 감지 시 "에러를 돌려주면서 폐기는 커밋"되는 동작처럼
 * 실제 커밋 여부가 중요한 부분을 보기 위해서다. 대신 테스트마다 테이블을 비운다.
 */
@SpringBootTest(properties = "umatgong.kakao.allowed-redirect-uris=" + AuthFlowIntegrationTest.REDIRECT_URI)
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class AuthFlowIntegrationTest {

	static final String REDIRECT_URI = "https://umatgong.test/auth/kakao/callback";

	private static final MockWebServer KAKAO = startKakaoStub();
	// 인증 서버(kauth.kakao.com). 웹 로그인의 코드 교환만 여기로 온다.
	private static final MockWebServer KAKAO_OAUTH = startKakaoStub();

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private KakaoProperties kakaoProperties;

	@AfterEach
	void cleanUp() throws InterruptedException {
		// 기록된 교환 요청이 다음 테스트에서 읽히지 않게 비운다
		while (KAKAO_OAUTH.takeRequest(0, TimeUnit.SECONDS) != null) {
			// drain
		}
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	@Test
	void 이메일_없이_카카오_회원번호만으로_가입된다() throws Exception {
		JsonNode data = login(4321L, "지현");

		assertThat(data.get("newUser").asBoolean()).isTrue();
		assertThat(data.at("/user/name").asText()).isEqualTo("지현");
		assertThat(jdbc.queryForObject("select kakao_id from users", Long.class)).isEqualTo(4321L);
		List<String> columns = jdbc.queryForList(
			"select column_name from information_schema.columns where table_name = 'users'", String.class);
		assertThat(columns).noneMatch(c -> c.contains("email"));
	}

	@Test
	void 같은_회원번호로_다시_로그인하면_같은_사용자이고_프로필이_갱신된다() throws Exception {
		JsonNode first = login(4321L, "지현");
		JsonNode second = login(4321L, "지현이");

		assertThat(second.get("newUser").asBoolean()).isFalse();
		assertThat(second.at("/user/id").asLong()).isEqualTo(first.at("/user/id").asLong());
		assertThat(second.at("/user/name").asText()).isEqualTo("지현이");
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
		// 기기마다 로그인 세션(리프레시 토큰)은 따로 생긴다.
		assertThat(jdbc.queryForObject("select count(distinct family_id) from refresh_tokens", Integer.class))
			.isEqualTo(2);
	}

	@Test
	void 회원번호가_다르면_다른_사용자다() throws Exception {
		long a = login(1L, "지현").at("/user/id").asLong();
		long b = login(2L, "민기").at("/user/id").asLong();

		assertThat(a).isNotEqualTo(b);
	}

	@Test
	void 리프레시_토큰은_DB에_원문이_아니라_해시로만_남는다() throws Exception {
		String refreshToken = login(4321L, "지현").get("refreshToken").asText();

		assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Integer.class,
			refreshToken)).isZero();
		assertThat(jdbc.queryForObject("select length(token_hash) from refresh_tokens", Integer.class)).isEqualTo(64);
	}

	@Test
	void 갱신하면_새_토큰이_나오고_이전_토큰은_재사용할_수_없다() throws Exception {
		String r1 = login(4321L, "지현").get("refreshToken").asText();

		String r2 = refresh(r1).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String next = objectMapper.readTree(r2).at("/data/refreshToken").asText();
		assertThat(next).isNotEqualTo(r1);

		refresh(r1)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	void 재사용이_감지되면_그_세션의_최신_토큰까지_폐기된다() throws Exception {
		String r1 = login(4321L, "지현").get("refreshToken").asText();
		String r2 = objectMapper.readTree(refresh(r1).andReturn().getResponse().getContentAsString())
			.at("/data/refreshToken").asText();

		refresh(r1).andExpect(status().isUnauthorized());

		// 재사용 응답은 에러였지만 family 폐기는 커밋돼 있어야 한다.
		refresh(r2)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
		assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where revoked_at is null", Integer.class))
			.isZero();
	}

	@Test
	void 한_기기의_재사용은_다른_기기의_로그인에_영향이_없다() throws Exception {
		String phone = login(4321L, "지현").get("refreshToken").asText();
		String web = login(4321L, "지현").get("refreshToken").asText();
		refresh(phone).andExpect(status().isOk());

		refresh(phone).andExpect(status().isUnauthorized());

		refresh(web).andExpect(status().isOk());
	}

	@Test
	void 로그아웃하면_그_리프레시_토큰으로_갱신할_수_없다() throws Exception {
		String refreshToken = login(4321L, "지현").get("refreshToken").asText();

		mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refreshToken + "\"}"))
			.andExpect(status().isOk());

		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	@Test
	void 발급된_액세스_토큰으로_인증이_필요한_경로를_통과한다() throws Exception {
		String accessToken = login(4321L, "지현").get("accessToken").asText();

		// 아직 인증이 필요한 API가 없으므로 없는 경로로 확인한다. 인증을 통과하면 401이 아니라 404다.
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 카카오가_토큰을_거부하면_가입되지_않는다() throws Exception {
		KAKAO.enqueue(json(401, "{\"code\": -401}"));

		mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON)
				.content("{\"kakaoAccessToken\":\"bad\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("KAKAO_INVALID_TOKEN"));
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
	}

	@Test
	void 웹_코드_로그인과_네이티브_토큰_로그인은_같은_사용자로_귀결된다() throws Exception {
		JsonNode web = loginWithCode(4321L, "지현");
		JsonNode nativeApp = login(4321L, "지현");

		assertThat(web.get("newUser").asBoolean()).isTrue();
		assertThat(nativeApp.get("newUser").asBoolean()).isFalse();
		assertThat(nativeApp.at("/user/id").asLong()).isEqualTo(web.at("/user/id").asLong());
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
	}

	@Test
	void 네이티브로_먼저_가입해도_웹_로그인은_같은_사용자다() throws Exception {
		JsonNode nativeApp = login(4321L, "지현");
		JsonNode web = loginWithCode(4321L, "지현");

		assertThat(web.get("newUser").asBoolean()).isFalse();
		assertThat(web.at("/user/id").asLong()).isEqualTo(nativeApp.at("/user/id").asLong());
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
	}

	@Test
	void 코드_교환에는_서버의_REST_API_키와_클라이언트가_보낸_redirectUri를_그대로_쓴다() throws Exception {
		loginWithCode(4321L, "지현");

		RecordedRequest exchange = KAKAO_OAUTH.takeRequest();
		assertThat(exchange.getTarget()).isEqualTo("/oauth/token");
		String form = exchange.getBody().utf8();
		assertThat(form).contains("client_id=" + kakaoProperties.restApiKey());
		assertThat(form).contains("redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8));
		assertThat(form).contains("code=code-4321");
	}

	@Test
	void 웹_로그인_토큰으로도_갱신된다() throws Exception {
		String refreshToken = loginWithCode(4321L, "지현").get("refreshToken").asText();

		refresh(refreshToken).andExpect(status().isOk());
	}

	@Test
	void 허용되지_않은_redirectUri는_카카오를_부르지_않고_가입도_하지_않는다() throws Exception {
		int kauthBefore = KAKAO_OAUTH.getRequestCount();
		int kapiBefore = KAKAO.getRequestCount();

		mockMvc.perform(post("/api/auth/kakao/code").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"code-1\",\"redirectUri\":\"https://evil.test/auth/kakao/callback\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("KAKAO_REDIRECT_URI_NOT_ALLOWED"));

		assertThat(KAKAO_OAUTH.getRequestCount()).isEqualTo(kauthBefore);
		assertThat(KAKAO.getRequestCount()).isEqualTo(kapiBefore);
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
	}

	@Test
	void 만료된_코드는_KAKAO_INVALID_TOKEN이고_가입되지_않는다() throws Exception {
		KAKAO_OAUTH.enqueue(json(400, "{\"error\": \"invalid_grant\", \"error_code\": \"KOE320\"}"));

		mockMvc.perform(post("/api/auth/kakao/code").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"used\",\"redirectUri\":\"" + REDIRECT_URI + "\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("KAKAO_INVALID_TOKEN"));
		KAKAO_OAUTH.takeRequest();
		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isZero();
	}

	// 웹 로그인: 인증 서버가 코드를 카카오 토큰으로 바꿔 주고, 이후는 네이티브와 같은 사용자 API 호출이 이어진다.
	private JsonNode loginWithCode(long kakaoId, String nickname) throws Exception {
		KAKAO_OAUTH.enqueue(json(200, "{\"token_type\": \"bearer\", \"access_token\": \"kakao-token-" + kakaoId
			+ "\", \"expires_in\": 21599}"));
		enqueueUserApi(kakaoId, nickname);
		String body = mockMvc.perform(post("/api/auth/kakao/code").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"code-" + kakaoId + "\",\"redirectUri\":\"" + REDIRECT_URI + "\"}"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("data");
	}

	private void enqueueUserApi(long kakaoId, String nickname) {
		KAKAO.enqueue(json(200, "{\"id\": " + kakaoId + ", \"app_id\": " + kakaoProperties.appId() + "}"));
		KAKAO.enqueue(json(200, "{\"id\": " + kakaoId + ", \"kakao_account\": {\"profile\": {\"nickname\": \""
			+ nickname + "\", \"is_default_image\": true}}}"));
	}

	private JsonNode login(long kakaoId, String nickname) throws Exception {
		enqueueUserApi(kakaoId, nickname);
		String body = mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON)
				.content("{\"kakaoAccessToken\":\"kakao-token-" + kakaoId + "\"}"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("data");
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}"));
	}

	private static MockResponse json(int code, String body) {
		return new MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build();
	}

	// 컨텍스트가 뜨기 전에 주소가 정해져 있어야 하므로 클래스 로딩 시점에 띄운다.
	private static MockWebServer startKakaoStub() {
		MockWebServer server = new MockWebServer();
		try {
			server.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return server;
	}

	/**
	 * 운영 코드의 kakaoApiRestClient(kapi.kakao.com)·kakaoOAuthRestClient(kauth.kakao.com)를 스텁 주소로 바꿔 끼운다.
	 * 같은 한정자에 @Primary를 붙여 KakaoAuthClient가 이 빈을 받게 한다.
	 */
	@TestConfiguration
	static class KakaoStubConfig {

		@Bean
		@Primary
		@Qualifier("kakaoApiRestClient")
		RestClient kakaoStubRestClient() {
			return RestClient.builder().baseUrl(KAKAO.url("/").toString()).build();
		}

		@Bean
		@Primary
		@Qualifier("kakaoOAuthRestClient")
		RestClient kakaoOAuthStubRestClient() {
			return RestClient.builder().baseUrl(KAKAO_OAUTH.url("/").toString()).build();
		}
	}
}

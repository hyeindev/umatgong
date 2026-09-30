package com.umatgong.global.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 카카오 사용자 API와 인증 서버를 로컬 HTTP 서버(MockWebServer)로 대신한다. 실제 카카오는 호출하지 않는다.
 */
class KakaoAuthClientTest {

	private static final long OUR_APP_ID = 1000L;
	private static final String REDIRECT_URI = "https://umatgong.test/auth/kakao/callback";

	private MockWebServer kakao;
	private MockWebServer kauth;
	private KakaoAuthClient client;

	@BeforeEach
	void setUp() throws IOException {
		kakao = new MockWebServer();
		kakao.start();
		kauth = new MockWebServer();
		kauth.start();
		client = clientWith(properties(null));
	}

	@AfterEach
	void tearDown() {
		kakao.close();
		kauth.close();
	}

	@Test
	void 토큰의_앱을_먼저_확인하고_사용자_정보를_가져온다() throws Exception {
		kakao.enqueue(json(200, tokenInfo(OUR_APP_ID)));
		kakao.enqueue(json(200, """
			{"id": 4321, "kakao_account": {"profile": {
			  "nickname": "지현", "profile_image_url": "https://img/p.jpg", "is_default_image": false}}}
			"""));

		KakaoUserInfo user = client.verifyAndFetchUser("user-token");

		assertThat(user).isEqualTo(new KakaoUserInfo(4321L, "지현", "https://img/p.jpg"));
		RecordedRequest first = kakao.takeRequest();
		RecordedRequest second = kakao.takeRequest();
		assertThat(first.getMethod()).isEqualTo("GET");
		assertThat(first.getTarget()).isEqualTo("/v1/user/access_token_info");
		assertThat(first.getHeaders().get("Authorization")).isEqualTo("Bearer user-token");
		assertThat(second.getTarget()).isEqualTo("/v2/user/me");
		assertThat(second.getHeaders().get("Authorization")).isEqualTo("Bearer user-token");
	}

	@Test
	void REST_API_키는_요청에_실리지_않는다() throws Exception {
		kakao.enqueue(json(200, tokenInfo(OUR_APP_ID)));
		kakao.enqueue(json(200, "{\"id\": 1}"));

		client.verifyAndFetchUser("user-token");

		for (int i = 0; i < 2; i++) {
			RecordedRequest request = kakao.takeRequest();
			assertThat(request.getHeaders().toString()).doesNotContain("rest-key");
			assertThat(request.getTarget()).doesNotContain("rest-key");
		}
	}

	@Test
	void 이메일_없이_회원번호만_와도_된다() {
		kakao.enqueue(json(200, tokenInfo(OUR_APP_ID)));
		// 동의항목이 닉네임·프로필 이미지뿐이면 kakao_account에 email이 없다.
		kakao.enqueue(json(200, """
			{"id": 55, "kakao_account": {"profile_nickname_needs_agreement": false,
			  "profile": {"nickname": "민기", "is_default_image": true}}}
			"""));

		KakaoUserInfo user = client.verifyAndFetchUser("user-token");

		assertThat(user.kakaoId()).isEqualTo(55L);
		assertThat(user.nickname()).isEqualTo("민기");
		assertThat(user.profileImageUrl()).isNull();
	}

	@Test
	void 프로필_동의가_없으면_회원번호만_돌려준다() {
		kakao.enqueue(json(200, tokenInfo(OUR_APP_ID)));
		kakao.enqueue(json(200, "{\"id\": 77}"));

		assertThat(client.verifyAndFetchUser("user-token")).isEqualTo(new KakaoUserInfo(77L, null, null));
	}

	@Test
	void 다른_앱에서_발급된_토큰이면_사용자_정보를_조회하지_않고_거부한다() {
		kakao.enqueue(json(200, tokenInfo(9999L)));

		assertError(() -> client.verifyAndFetchUser("user-token"), ErrorCode.KAKAO_INVALID_TOKEN);
		assertThat(kakao.getRequestCount()).isEqualTo(1);
	}

	@Test
	void 카카오가_401을_주면_KAKAO_INVALID_TOKEN() {
		kakao.enqueue(json(401, "{\"msg\": \"this access token does not exist\", \"code\": -401}"));

		assertError(() -> client.verifyAndFetchUser("expired"), ErrorCode.KAKAO_INVALID_TOKEN);
	}

	@Test
	void 카카오_서버_오류는_KAKAO_UNAVAILABLE() {
		kakao.enqueue(json(500, "{}"));

		assertError(() -> client.verifyAndFetchUser("user-token"), ErrorCode.KAKAO_UNAVAILABLE);
	}

	@Test
	void 응답이_깨져_있으면_KAKAO_UNAVAILABLE() {
		kakao.enqueue(json(200, "<html>maintenance</html>"));

		assertError(() -> client.verifyAndFetchUser("user-token"), ErrorCode.KAKAO_UNAVAILABLE);
	}

	@Test
	void 응답이_늦으면_기다리지_않고_KAKAO_UNAVAILABLE() {
		kakao.enqueue(new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
			.body(tokenInfo(OUR_APP_ID)).headersDelay(2, TimeUnit.SECONDS).build());

		assertError(() -> client.verifyAndFetchUser("user-token"), ErrorCode.KAKAO_UNAVAILABLE);
	}

	@Test
	void 앱_ID_없이는_생성되지_않는다() {
		assertThatThrownBy(() -> new KakaoAuthClient(RestClient.create(), RestClient.create(),
			new KakaoProperties("rest-key", null, null, List.of())))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void REST_API_키_없이는_생성되지_않는다() {
		assertThatThrownBy(() -> new KakaoAuthClient(RestClient.create(), RestClient.create(),
			new KakaoProperties(" ", 1L, null, List.of())))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 설정_객체를_출력해도_REST_API_키와_Client_Secret이_보이지_않는다() {
		String printed = new KakaoProperties("secret-rest-key", 1L, "secret-client", List.of(REDIRECT_URI))
			.toString();

		assertThat(printed).doesNotContain("secret-rest-key").doesNotContain("secret-client");
	}

	// ── 웹 로그인: 인가 코드 → 카카오 액세스 토큰 ──

	@Test
	void 인가_코드를_REST_API_키로_카카오_액세스_토큰과_바꾼다() throws Exception {
		kauth.enqueue(json(200, oauthToken("kakao-access-token")));

		String token = client.exchangeCode("auth-code", REDIRECT_URI);

		assertThat(token).isEqualTo("kakao-access-token");
		RecordedRequest request = kauth.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getTarget()).isEqualTo("/oauth/token");
		assertThat(request.getHeaders().get("Content-Type")).startsWith("application/x-www-form-urlencoded");
		assertThat(formOf(request)).containsExactly(
			"grant_type=authorization_code",
			"client_id=rest-key",
			"redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8),
			"code=auth-code");
		// 키는 본문에만 싣는다. 주소·헤더에 남지 않는다.
		assertThat(request.getTarget()).doesNotContain("rest-key");
		assertThat(request.getHeaders().toString()).doesNotContain("rest-key");
		// 사용자 API는 부르지 않는다. 토큰 검증은 로그인 경로(AuthService)가 이어서 한다.
		assertThat(kakao.getRequestCount()).isZero();
	}

	@Test
	void Client_Secret을_설정하면_교환_요청에_함께_보낸다() throws Exception {
		client = clientWith(properties("client-secret"));
		kauth.enqueue(json(200, oauthToken("t")));

		client.exchangeCode("auth-code", REDIRECT_URI);

		assertThat(formOf(kauth.takeRequest())).contains("client_secret=client-secret");
	}

	@Test
	void Client_Secret이_없으면_보내지_않는다() throws Exception {
		kauth.enqueue(json(200, oauthToken("t")));

		client.exchangeCode("auth-code", REDIRECT_URI);

		assertThat(formOf(kauth.takeRequest())).noneMatch(field -> field.startsWith("client_secret="));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"https://evil.test/auth/kakao/callback",
		"https://umatgong.test/auth/kakao/callback/",
		"https://umatgong.test/auth/kakao/callback?next=https://evil.test",
		"https://umatgong.test/auth/kakao/callback.evil.test",
		"http://umatgong.test/auth/kakao/callback",
		"HTTPS://UMATGONG.TEST/auth/kakao/callback",
		"https://umatgong.test/auth/kakao",
		""
	})
	void 허용_목록과_정확히_같지_않은_redirectUri는_카카오를_부르지_않고_거부한다(String redirectUri) {
		assertError(() -> client.exchangeCode("auth-code", redirectUri), ErrorCode.KAKAO_REDIRECT_URI_NOT_ALLOWED);
		assertThat(kauth.getRequestCount()).isZero();
	}

	@Test
	void 허용_목록이_비어_있으면_모든_redirectUri를_거부한다() {
		client = clientWith(new KakaoProperties("rest-key", OUR_APP_ID, null, List.of()));

		assertError(() -> client.exchangeCode("auth-code", REDIRECT_URI), ErrorCode.KAKAO_REDIRECT_URI_NOT_ALLOWED);
		assertThat(kauth.getRequestCount()).isZero();
	}

	@Test
	void 만료되거나_이미_쓴_코드는_KAKAO_INVALID_TOKEN() {
		kauth.enqueue(json(400, """
			{"error": "invalid_grant", "error_description": "authorization code not found for code=...",
			 "error_code": "KOE320"}
			"""));

		assertError(() -> client.exchangeCode("used-code", REDIRECT_URI), ErrorCode.KAKAO_INVALID_TOKEN);
	}

	@Test
	void authorize와_redirectUri가_다르면_KAKAO_INVALID_TOKEN() {
		kauth.enqueue(json(400, """
			{"error": "invalid_grant", "error_description": "Redirect URI mismatch.", "error_code": "KOE006"}
			"""));

		assertError(() -> client.exchangeCode("auth-code", REDIRECT_URI), ErrorCode.KAKAO_INVALID_TOKEN);
	}

	@Test
	void 카카오가_우리_키를_거부하면_사용자_탓이_아니므로_INTERNAL_ERROR() {
		kauth.enqueue(json(401, """
			{"error": "invalid_client", "error_description": "Bad client credentials", "error_code": "KOE010"}
			"""));

		assertError(() -> client.exchangeCode("auth-code", REDIRECT_URI), ErrorCode.INTERNAL_ERROR);
	}

	@Test
	void 인증_서버_오류는_KAKAO_UNAVAILABLE() {
		kauth.enqueue(json(503, "{}"));

		assertError(() -> client.exchangeCode("auth-code", REDIRECT_URI), ErrorCode.KAKAO_UNAVAILABLE);
	}

	@Test
	void 인증_서버가_늦으면_기다리지_않고_KAKAO_UNAVAILABLE() {
		kauth.enqueue(new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
			.body(oauthToken("t")).headersDelay(2, TimeUnit.SECONDS).build());

		assertError(() -> client.exchangeCode("auth-code", REDIRECT_URI), ErrorCode.KAKAO_UNAVAILABLE);
	}

	@Test
	void 응답에_액세스_토큰이_없으면_KAKAO_UNAVAILABLE() {
		kauth.enqueue(json(200, "{\"token_type\": \"bearer\"}"));

		assertError(() -> client.exchangeCode("auth-code", REDIRECT_URI), ErrorCode.KAKAO_UNAVAILABLE);
	}

	private KakaoProperties properties(String clientSecret) {
		return new KakaoProperties("rest-key", OUR_APP_ID, clientSecret, List.of(REDIRECT_URI));
	}

	private KakaoAuthClient clientWith(KakaoProperties properties) {
		return new KakaoAuthClient(restClientFor(kakao), restClientFor(kauth), properties);
	}

	private static RestClient restClientFor(MockWebServer server) {
		return RestClient.builder()
			.baseUrl(server.url("/").toString())
			.requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(Duration.ofSeconds(1))
				.withReadTimeout(Duration.ofMillis(500))))
			.build();
	}

	private static List<String> formOf(RecordedRequest request) {
		return List.of(request.getBody().utf8().split("&"));
	}

	private static String oauthToken(String accessToken) {
		return "{\"token_type\": \"bearer\", \"access_token\": \"" + accessToken
			+ "\", \"expires_in\": 21599, \"refresh_token\": \"kakao-refresh\", \"refresh_token_expires_in\": 5183999}";
	}

	private static String tokenInfo(long appId) {
		return "{\"id\": 4321, \"expires_in\": 7199, \"app_id\": " + appId + "}";
	}

	private static MockResponse json(int code, String body) {
		return new MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build();
	}

	private static void assertError(ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(expected);
	}
}

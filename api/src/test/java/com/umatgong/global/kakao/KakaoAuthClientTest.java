package com.umatgong.global.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 카카오 사용자 API를 로컬 HTTP 서버(MockWebServer)로 대신한다. 실제 카카오는 호출하지 않는다.
 */
class KakaoAuthClientTest {

	private static final long OUR_APP_ID = 1000L;

	private MockWebServer kakao;
	private KakaoAuthClient client;

	@BeforeEach
	void setUp() throws IOException {
		kakao = new MockWebServer();
		kakao.start();
		RestClient restClient = RestClient.builder()
			.baseUrl(kakao.url("/").toString())
			.requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(Duration.ofSeconds(1))
				.withReadTimeout(Duration.ofMillis(500))))
			.build();
		client = new KakaoAuthClient(restClient, new KakaoProperties("rest-key", OUR_APP_ID));
	}

	@AfterEach
	void tearDown() {
		kakao.close();
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
		assertThatThrownBy(() -> new KakaoAuthClient(RestClient.create(), new KakaoProperties("rest-key", null)))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void 설정_객체를_출력해도_REST_API_키가_보이지_않는다() {
		assertThat(new KakaoProperties("secret-rest-key", 1L).toString()).doesNotContain("secret-rest-key");
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

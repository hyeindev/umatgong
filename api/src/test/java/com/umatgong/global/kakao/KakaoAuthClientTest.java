package com.umatgong.global.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

class KakaoAuthClientTest {

	private static final long OUR_APP_ID = 1000L;
	private static final String BASE = "https://kapi.kakao.com";

	private MockRestServiceServer server;
	private KakaoAuthClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new KakaoAuthClient(builder.build(), new KakaoProperties("rest-key", OUR_APP_ID));
	}

	@Test
	void 우리_앱의_토큰이면_회원번호와_프로필을_돌려준다() {
		expectTokenInfo(OUR_APP_ID);
		server.expect(requestTo(BASE + "/v2/user/me"))
			.andExpect(header("Authorization", "Bearer user-token"))
			.andRespond(withSuccess("""
				{"id": 4321, "kakao_account": {"profile": {
				  "nickname": "지현", "profile_image_url": "https://img/p.jpg", "is_default_image": false}}}
				""", MediaType.APPLICATION_JSON));

		KakaoUserInfo user = client.verifyAndFetchUser("user-token");

		assertThat(user).isEqualTo(new KakaoUserInfo(4321L, "지현", "https://img/p.jpg"));
		server.verify();
	}

	@Test
	void 카카오_기본_이미지는_사진_없음으로_본다() {
		expectTokenInfo(OUR_APP_ID);
		server.expect(requestTo(BASE + "/v2/user/me")).andRespond(withSuccess("""
			{"id": 1, "kakao_account": {"profile": {
			  "nickname": "민기", "profile_image_url": "https://img/default.jpg", "is_default_image": true}}}
			""", MediaType.APPLICATION_JSON));

		assertThat(client.verifyAndFetchUser("user-token").profileImageUrl()).isNull();
	}

	@Test
	void 다른_앱에서_발급된_토큰이면_사용자_정보를_조회하지_않고_거부한다() {
		expectTokenInfo(9999L);

		assertInvalidToken(() -> client.verifyAndFetchUser("user-token"));
		// /v2/user/me를 기대하지 않았으므로 호출됐다면 verify가 실패한다.
		server.verify();
	}

	@Test
	void 카카오가_401을_주면_KAKAO_INVALID_TOKEN이다() {
		server.expect(requestTo(BASE + "/v1/user/access_token_info")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertInvalidToken(() -> client.verifyAndFetchUser("expired"));
	}

	@Test
	void 카카오_장애는_KAKAO_UNAVAILABLE이다() {
		server.expect(requestTo(BASE + "/v1/user/access_token_info")).andRespond(withServerError());

		assertThatThrownBy(() -> client.verifyAndFetchUser("user-token"))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.KAKAO_UNAVAILABLE);
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

	private void expectTokenInfo(long appId) {
		server.expect(requestTo(BASE + "/v1/user/access_token_info"))
			.andExpect(header("Authorization", "Bearer user-token"))
			.andRespond(withSuccess("{\"id\": 4321, \"expires_in\": 7199, \"app_id\": " + appId + "}",
				MediaType.APPLICATION_JSON));
	}

	private static void assertInvalidToken(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.KAKAO_INVALID_TOKEN);
	}
}

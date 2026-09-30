package com.umatgong.global.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
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
import com.umatgong.global.geo.Coordinate;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.HttpUrl;

/**
 * 카카오 로컬 API를 MockWebServer로 대신한다. 실제 카카오는 호출하지 않는다.
 */
class KakaoLocalClientTest {

	// 망원동. 위도(37.x)와 경도(126.x)가 확연히 달라 뒤바뀌면 바로 드러난다.
	private static final Coordinate MANGWON = Coordinate.of(37.5556, 126.9106);

	private MockWebServer kakao;
	private KakaoLocalClient client;

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
		client = new KakaoLocalClient(restClient, new KakaoProperties("test-rest-key", 1L));
	}

	@AfterEach
	void tearDown() {
		kakao.close();
	}

	// ---- 좌표 변환 -----------------------------------------------------------------------------

	@Test
	void 요청할_때_x에는_경도_y에는_위도가_들어간다() throws Exception {
		kakao.enqueue(json(200, "{\"documents\": []}"));

		client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500);

		HttpUrl url = kakao.takeRequest().getUrl();
		assertThat(url.queryParameter("x")).isEqualTo("126.9106");
		assertThat(url.queryParameter("y")).isEqualTo("37.5556");
	}

	@Test
	void 응답의_y는_위도로_x는_경도로_읽는다() {
		// 카카오 문서 순서 그대로 x(경도)가 먼저 온다.
		kakao.enqueue(json(200, documents(doc("1", "망원동 김반장", "FD6", "126.9106", "37.5556"))));

		KakaoPlace place = client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500).get(0);

		assertThat(place.coordinate().lat()).isEqualTo(37.5556);
		assertThat(place.coordinate().lng()).isEqualTo(126.9106);
	}

	@Test
	void 요청한_좌표와_응답_좌표를_왕복해도_뒤바뀌지_않는다() throws Exception {
		// 카카오가 "요청 받은 x, y를 그대로 돌려준다"고 흉내 낸다. 우리 쪽 변환이 양방향에서 맞아야만 원래 좌표가 나온다.
		kakao.enqueue(json(200, "{\"documents\": []}"));
		client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500);
		HttpUrl sent = kakao.takeRequest().getUrl();

		kakao.enqueue(json(200, documents(doc("1", "echo", "FD6", sent.queryParameter("x"), sent.queryParameter("y")))));
		KakaoPlace echoed = client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500).get(0);

		assertThat(echoed.coordinate()).isEqualTo(MANGWON);
	}

	@Test
	void 키워드_검색도_x에_경도_y에_위도를_넣는다() throws Exception {
		kakao.enqueue(json(200, "{\"documents\": []}"));

		client.searchByKeyword("곱창", MANGWON);

		HttpUrl url = kakao.takeRequest().getUrl();
		assertThat(url.queryParameter("x")).isEqualTo("126.9106");
		assertThat(url.queryParameter("y")).isEqualTo("37.5556");
	}

	// ---- 요청 형태 ---------------------------------------------------------------------------

	@Test
	void 카테고리_검색_요청() throws Exception {
		kakao.enqueue(json(200, "{\"documents\": []}"));

		client.searchByCategory(KakaoCategory.CAFE, MANGWON, 670);

		RecordedRequest request = kakao.takeRequest();
		assertThat(request.getUrl().encodedPath()).isEqualTo("/v2/local/search/category.json");
		assertThat(request.getUrl().queryParameter("category_group_code")).isEqualTo("CE7");
		assertThat(request.getUrl().queryParameter("radius")).isEqualTo("670");
		assertThat(request.getUrl().queryParameter("sort")).isEqualTo("distance");
		assertThat(request.getHeaders().get("Authorization")).isEqualTo("KakaoAK test-rest-key");
	}

	@Test
	void 키워드는_한글_그대로_전달되고_좌표가_없으면_x_y를_보내지_않는다() throws Exception {
		kakao.enqueue(json(200, "{\"documents\": []}"));

		client.searchByKeyword("망원동 김반장", null);

		HttpUrl url = kakao.takeRequest().getUrl();
		assertThat(url.encodedPath()).isEqualTo("/v2/local/search/keyword.json");
		assertThat(url.queryParameter("query")).isEqualTo("망원동 김반장");
		assertThat(url.queryParameter("x")).isNull();
		assertThat(url.queryParameter("y")).isNull();
	}

	// ---- 응답 해석 ---------------------------------------------------------------------------

	@Test
	void 도로명_주소를_우선하고_업종_태그를_뽑는다() {
		kakao.enqueue(json(200, """
			{"documents": [{"id": "1", "place_name": "망원동 김반장",
			  "category_name": "음식점 > 한식 > 육류,고기 > 곱창,막창", "category_group_code": "FD6",
			  "address_name": "서울 마포구 망원동 1", "road_address_name": "서울 마포구 포은로 1",
			  "x": "126.9106", "y": "37.5556", "phone": "02-000-0000"}]}
			"""));

		KakaoPlace place = client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500).get(0);

		assertThat(place.externalId()).isEqualTo("1");
		assertThat(place.address()).isEqualTo("서울 마포구 포은로 1");
		assertThat(place.categoryTags()).containsExactly("한식", "육류,고기", "곱창,막창");
	}

	@Test
	void 도로명_주소가_없으면_지번_주소를_쓴다() {
		kakao.enqueue(json(200, """
			{"documents": [{"id": "1", "place_name": "x", "category_group_code": "FD6",
			  "address_name": "서울 마포구 망원동 1", "road_address_name": "", "x": "126.9", "y": "37.5"}]}
			"""));

		assertThat(client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500).get(0).address())
			.isEqualTo("서울 마포구 망원동 1");
	}

	// ---- 키 노출 ----------------------------------------------------------------------------

	@Test
	void REST_API_키는_헤더에만_실리고_URL에는_없다() throws Exception {
		kakao.enqueue(json(200, "{\"documents\": []}"));

		client.searchByKeyword("곱창", MANGWON);

		RecordedRequest request = kakao.takeRequest();
		assertThat(request.getTarget()).doesNotContain("test-rest-key");
	}

	@Test
	void REST_API_키가_없으면_만들어지지_않는다() {
		assertThatThrownBy(() -> new KakaoLocalClient(RestClient.create(), new KakaoProperties(" ", 1L)))
			.isInstanceOf(IllegalStateException.class);
	}

	// ---- 오류 ------------------------------------------------------------------------------

	@Test
	void 키_오류_쿼터_초과_서버_오류는_모두_KAKAO_UNAVAILABLE이다() {
		for (int status : List.of(401, 403, 429, 500)) {
			kakao.enqueue(json(status, "{\"errorType\": \"x\", \"message\": \"x\"}"));
			assertError(() -> client.searchByCategory(KakaoCategory.RESTAURANT, MANGWON, 500),
				ErrorCode.KAKAO_UNAVAILABLE);
		}
	}

	@Test
	void 응답이_늦으면_KAKAO_UNAVAILABLE이다() {
		kakao.enqueue(new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
			.body("{\"documents\": []}").headersDelay(2, TimeUnit.SECONDS).build());

		assertError(() -> client.searchByKeyword("곱창", null), ErrorCode.KAKAO_UNAVAILABLE);
	}

	@Test
	void 좌표가_깨진_응답은_KAKAO_UNAVAILABLE이다() {
		kakao.enqueue(json(200, documents(doc("1", "x", "FD6", "not-a-number", "37.5"))));

		assertError(() -> client.searchByKeyword("곱창", null), ErrorCode.KAKAO_UNAVAILABLE);
	}

	static String doc(String id, String name, String group, String x, String y) {
		return "{\"id\": \"" + id + "\", \"place_name\": \"" + name + "\", \"category_name\": \"음식점 > 한식\", "
			+ "\"category_group_code\": \"" + group + "\", \"address_name\": \"주소\", \"road_address_name\": \"\", "
			+ "\"x\": \"" + x + "\", \"y\": \"" + y + "\"}";
	}

	static String documents(String... docs) {
		return "{\"documents\": [" + String.join(",", docs) + "], \"meta\": {\"total_count\": " + docs.length + "}}";
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

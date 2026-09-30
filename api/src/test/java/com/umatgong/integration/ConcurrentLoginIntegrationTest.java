package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umatgong.global.kakao.KakaoProperties;

import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * 같은 신규 사용자가 동시에 로그인해도(버튼 연타, 재시도) 전부 성공하고 사용자는 한 명만 생기는지 본다.
 * 수정 전에는 늦게 도착한 요청이 회원번호 중복으로 500을 받았다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ConcurrentLoginIntegrationTest {

	private static final int CONCURRENT_REQUESTS = 4;
	private static final long KAKAO_ID = 8888L;
	private static final MockWebServer KAKAO = startKakaoStub();

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private KakaoProperties kakaoProperties;

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	@RepeatedTest(5)
	void 같은_신규_사용자가_동시에_로그인해도_모두_성공하고_사용자는_한_명이다() throws Exception {
		appId = kakaoProperties.appId();
		ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
		for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
			futures.add(pool.submit(() -> {
				start.await();
				return mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON)
					.content("{\"kakaoAccessToken\":\"same-user\"}")).andReturn().getResponse();
			}));
		}
		start.countDown();

		List<JsonNode> bodies = new ArrayList<>();
		for (Future<MockHttpServletResponse> future : futures) {
			MockHttpServletResponse response = future.get(30, TimeUnit.SECONDS);
			assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
			bodies.add(objectMapper.readTree(response.getContentAsString()).get("data"));
		}
		pool.shutdown();

		assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
		assertThat(bodies).extracting(b -> b.at("/user/id").asLong()).containsOnly(bodies.get(0).at("/user/id").asLong());
		assertThat(bodies).filteredOn(b -> b.get("newUser").asBoolean()).hasSize(1);
		// 요청마다 로그인 세션이 하나씩 생긴다 (실패한 첫 시도의 토큰은 롤백돼 남지 않는다).
		assertThat(jdbc.queryForObject("select count(*) from refresh_tokens", Integer.class))
			.isEqualTo(CONCURRENT_REQUESTS);
	}

	private static volatile Long appId;

	// 요청 순서와 무관하게 경로로 응답한다. 약간 늦게 답해 요청들이 동시에 "사용자 없음"을 보게 만든다.
	private static MockWebServer startKakaoStub() {
		MockWebServer server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
				Thread.sleep(200);
				String body = request.getTarget().contains("access_token_info")
					? "{\"id\": " + KAKAO_ID + ", \"app_id\": " + appId + "}"
					: "{\"id\": " + KAKAO_ID + ", \"kakao_account\": {\"profile\": {\"nickname\": \"동시\"}}}";
				return new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json")
					.body(body).build();
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
		@Qualifier("kakaoApiRestClient")
		RestClient kakaoStubRestClient() {
			return RestClient.builder().baseUrl(KAKAO.url("/").toString()).build();
		}
	}
}

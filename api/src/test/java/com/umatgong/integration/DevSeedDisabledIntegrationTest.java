package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.umatgong.domain.dev.controller.DevSeedController;
import com.umatgong.domain.dev.service.DevSeedService;
import com.umatgong.domain.user.entity.User;
import com.umatgong.domain.user.repository.UserRepository;
import com.umatgong.global.security.JwtProvider;

/**
 * 플래그가 꺼져 있으면(기본값) 토큰이 맞아도 시드 API가 없는 경로다.
 */
@SpringBootTest(properties = "umatgong.dev.seed.token=" + DevSeedDisabledIntegrationTest.TOKEN)
@AutoConfigureMockMvc
@EnabledIf("com.umatgong.integration.TestDatabase#enabled")
class DevSeedDisabledIntegrationTest {

	static final String TOKEN = "test-dev-seed-token-0123456789abcdef";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtProvider jwtProvider;

	@AfterEach
	void cleanUp() {
		jdbc.execute("truncate refresh_tokens, visit_photos, visits, club_members, places, clubs, users "
			+ "restart identity cascade");
	}

	@Test
	void 기본값이면_컨트롤러와_서비스가_등록되지_않는다() {
		assertThat(context.getBeanNamesForType(DevSeedController.class)).isEmpty();
		assertThat(context.getBeanNamesForType(DevSeedService.class)).isEmpty();
	}

	@Test
	void 토큰이_맞아도_없는_경로와_같은_404() throws Exception {
		String bearer = "Bearer " + jwtProvider.createAccessToken(
			userRepository.save(User.signUpWithKakao(1L, "지현", null)).getId());

		MvcResult seed = mockMvc.perform(post(DevSeedController.PATH)
			.header(HttpHeaders.AUTHORIZATION, bearer)
			.header(DevSeedController.TOKEN_HEADER, TOKEN)).andReturn();
		MvcResult unknown = mockMvc.perform(post("/api/dev/nope").header(HttpHeaders.AUTHORIZATION, bearer))
			.andReturn();

		assertThat(seed.getResponse().getStatus()).isEqualTo(404);
		assertThat(seed.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString());
		assertThat(jdbc.queryForObject("select count(*) from clubs", Integer.class)).isZero();
	}
}

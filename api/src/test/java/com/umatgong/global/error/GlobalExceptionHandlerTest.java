package com.umatgong.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void 비즈니스_예외는_에러코드의_상태와_코드로_응답한다() throws Exception {
		mockMvc.perform(get("/test/forbidden"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void 검증_실패는_INVALID_INPUT과_필드_오류를_돌려준다() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));
	}

	@Test
	void 읽을_수_없는_본문은_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
	}

	@Test
	void 예상하지_못한_예외는_내부_메시지를_노출하지_않는다() throws Exception {
		mockMvc.perform(get("/test/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.error.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
	}

	@RestController
	static class TestController {

		@GetMapping("/test/forbidden")
		void forbidden() {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		@PostMapping("/test/validate")
		void validate(@Valid @RequestBody Request request) {
		}

		@GetMapping("/test/boom")
		void boom() {
			throw new IllegalStateException("db password is hunter2");
		}

		record Request(@NotBlank String name) {
		}
	}
}

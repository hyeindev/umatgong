package com.umatgong.domain.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umatgong.domain.place.dto.PlaceResponse;
import com.umatgong.domain.place.service.PlaceService;
import com.umatgong.global.config.CorsConfig;
import com.umatgong.global.config.CorsProperties;
import com.umatgong.global.config.SecurityConfig;
import com.umatgong.global.geo.Coordinate;
import com.umatgong.global.security.JwtProvider;

@WebMvcTest(PlaceController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = "umatgong.cors.allowed-origins=http://localhost:8081")
class PlaceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlaceService placeService;

	@MockitoBean
	private JwtProvider jwtProvider;

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5").param("lng", "126.9"))
			.andExpect(status().isUnauthorized());
		verifyNoInteractions(placeService);
	}

	@Test
	@WithMockUser
	void 주변_검색은_좌표를_lat_lng_그대로_서비스에_넘기고_반경_기본값은_500이다() throws Exception {
		given(placeService.nearby(any(), eq(500))).willReturn(List.of(new PlaceResponse(1L, "망원동 김반장", "주소",
			"곱창,막창", new PlaceResponse.CoordinateResponse(37.5556, 126.9106), 120)));

		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5556").param("lng", "126.9106"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].coordinate.lat").value(37.5556))
			.andExpect(jsonPath("$.data[0].coordinate.lng").value(126.9106))
			.andExpect(jsonPath("$.data[0].distanceMeters").value(120))
			// 카카오 표기(x/y)는 응답에 나타나지 않는다.
			.andExpect(jsonPath("$.data[0].coordinate.x").doesNotExist())
			.andExpect(jsonPath("$.data[0].externalId").doesNotExist());
		verify(placeService).nearby(Coordinate.of(37.5556, 126.9106), 500);
	}

	@Test
	@WithMockUser
	void 범위를_벗어난_좌표와_반경은_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(get("/api/places/nearby").param("lat", "91").param("lng", "126.9"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5").param("lng", "181"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5").param("lng", "126.9").param("radius", "0"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5").param("lng", "126.9").param("radius", "20001"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		mockMvc.perform(get("/api/places/nearby").param("lat", "37.5"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		verifyNoInteractions(placeService);
	}

	@Test
	@WithMockUser
	void 키워드_검색은_좌표_없이도_된다() throws Exception {
		given(placeService.search(eq("곱창"), isNull())).willReturn(List.of());

		mockMvc.perform(get("/api/places/search").param("query", "곱창"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	@WithMockUser
	void 키워드가_비었거나_좌표가_한쪽만_오면_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(get("/api/places/search").param("query", " "))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		mockMvc.perform(get("/api/places/search"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		mockMvc.perform(get("/api/places/search").param("query", "곱창").param("lat", "37.5"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
		verifyNoInteractions(placeService);
	}
}

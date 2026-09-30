package com.umatgong.domain.place.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.place.dto.PlaceResponse;
import com.umatgong.domain.place.service.PlaceService;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.geo.Coordinate;
import com.umatgong.global.response.ApiResponse;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

	private final PlaceService placeService;

	@GetMapping("/nearby")
	public ApiResponse<List<PlaceResponse>> nearby(
		@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
		@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
		// 카카오 카테고리 검색의 반경 상한이 20km다.
		@RequestParam(defaultValue = "500") @Min(1) @Max(20_000) int radius) {
		return ApiResponse.ok(placeService.nearby(Coordinate.of(lat, lng), radius));
	}

	@GetMapping("/search")
	public ApiResponse<List<PlaceResponse>> search(
		@RequestParam @NotBlank @Size(max = 100) String query,
		@RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double lat,
		@RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double lng) {
		if ((lat == null) != (lng == null)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "lat과 lng는 함께 보내야 합니다.");
		}
		Coordinate center = lat == null ? null : Coordinate.of(lat, lng);
		return ApiResponse.ok(placeService.search(query, center));
	}
}

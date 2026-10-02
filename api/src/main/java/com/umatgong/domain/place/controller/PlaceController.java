package com.umatgong.domain.place.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.place.dto.CreateCustomPlaceRequest;
import com.umatgong.domain.place.dto.PlaceResponse;
import com.umatgong.domain.place.service.PlaceService;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;
import com.umatgong.global.geo.Coordinate;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.CustomUserDetails;

import jakarta.validation.Valid;

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
	public ApiResponse<List<PlaceResponse>> nearby(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
		@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
		// 카카오 카테고리 검색의 반경 상한이 20km다.
		@RequestParam(defaultValue = "500") @Min(1) @Max(20_000) int radius) {
		return ApiResponse.ok(placeService.nearby(principal.getUserId(), Coordinate.of(lat, lng), radius));
	}

	@GetMapping("/search")
	public ApiResponse<List<PlaceResponse>> search(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestParam @NotBlank @Size(max = 100) String query,
		@RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double lat,
		@RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double lng) {
		if ((lat == null) != (lng == null)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "lat과 lng는 함께 보내야 합니다.");
		}
		Coordinate center = lat == null ? null : Coordinate.of(lat, lng);
		return ApiResponse.ok(placeService.search(principal.getUserId(), query, center));
	}

	/** 「여기 없어요」— 카카오에 없는 가게를 클럽 전용으로 등록한다 */
	@PostMapping("/custom")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PlaceResponse> createCustom(@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody CreateCustomPlaceRequest request) {
		return ApiResponse.ok(placeService.createCustom(principal.getUserId(), request));
	}
}

package com.umatgong.domain.visit.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.visit.dto.CreateVisitRequest;
import com.umatgong.domain.visit.dto.UpdateVisitRequest;
import com.umatgong.domain.visit.dto.VisitPageResponse;
import com.umatgong.domain.visit.dto.VisitPinResponse;
import com.umatgong.domain.visit.dto.VisitResponse;
import com.umatgong.domain.visit.entity.Rating;
import com.umatgong.domain.visit.service.VisitService;
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
import lombok.RequiredArgsConstructor;

// 권한 판단은 VisitService가 한다. 여기서는 입력 형식만 본다.
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

	private final VisitService visitService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<VisitResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody CreateVisitRequest request) {
		return ApiResponse.ok(visitService.create(principal.getUserId(), request));
	}

	@GetMapping("/map")
	public ApiResponse<List<VisitPinResponse>> map(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestParam @DecimalMin("-90") @DecimalMax("90") double swLat,
		@RequestParam @DecimalMin("-180") @DecimalMax("180") double swLng,
		@RequestParam @DecimalMin("-90") @DecimalMax("90") double neLat,
		@RequestParam @DecimalMin("-180") @DecimalMax("180") double neLng,
		@RequestParam(required = false) List<Long> clubIds) {
		if (swLat > neLat || swLng > neLng) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "sw는 ne보다 남서쪽이어야 합니다.");
		}
		return ApiResponse.ok(visitService.pins(principal.getUserId(), Coordinate.of(swLat, swLng),
			Coordinate.of(neLat, neLng), clubIds));
	}

	@GetMapping("/nearby")
	public ApiResponse<List<VisitResponse>> nearby(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
		@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
		@RequestParam(defaultValue = "1000") @Min(1) @Max(20_000) int radius,
		@RequestParam(required = false) List<Rating> rating) {
		return ApiResponse.ok(visitService.nearby(principal.getUserId(), Coordinate.of(lat, lng), radius, rating));
	}

	@GetMapping("/me")
	public ApiResponse<VisitPageResponse> mine(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false) @Min(1) @Max(100) Integer size) {
		return ApiResponse.ok(visitService.mine(principal.getUserId(), cursor, size));
	}

	@PatchMapping("/{visitId}")
	public ApiResponse<VisitResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long visitId, @Valid @RequestBody UpdateVisitRequest request) {
		return ApiResponse.ok(visitService.update(principal.getUserId(), visitId, request));
	}

	@DeleteMapping("/{visitId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long visitId) {
		visitService.delete(principal.getUserId(), visitId);
		return ApiResponse.ok();
	}
}

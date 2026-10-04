package com.umatgong.domain.scrap.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.scrap.dto.ScrapResponse;
import com.umatgong.domain.scrap.dto.ScrapStatusResponse;
import com.umatgong.domain.scrap.service.ScrapService;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScrapController {

	private final ScrapService scrapService;

	@GetMapping("/api/places/{placeId}/scrap")
	public ApiResponse<ScrapStatusResponse> status(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long placeId) {
		return ApiResponse.ok(scrapService.status(principal.getUserId(), placeId));
	}

	// 여러 번 불러도 결과가 같다 (이미 스크랩했으면 그대로)
	@PutMapping("/api/places/{placeId}/scrap")
	public ApiResponse<ScrapStatusResponse> add(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long placeId) {
		return ApiResponse.ok(scrapService.add(principal.getUserId(), placeId));
	}

	@DeleteMapping("/api/places/{placeId}/scrap")
	public ApiResponse<ScrapStatusResponse> remove(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long placeId) {
		return ApiResponse.ok(scrapService.remove(principal.getUserId(), placeId));
	}

	@GetMapping("/api/scraps")
	public ApiResponse<List<ScrapResponse>> mine(@AuthenticationPrincipal CustomUserDetails principal) {
		return ApiResponse.ok(scrapService.mine(principal.getUserId()));
	}
}

package com.umatgong.domain.visit.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.visit.dto.VisitResponse;
import com.umatgong.domain.visit.service.VisitService;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

// 장소 상세 화면의 방문 기록. 기록 도메인이 가지므로 장소 컨트롤러가 아니라 여기 둔다.
@RestController
@RequiredArgsConstructor
public class PlaceVisitController {

	private final VisitService visitService;

	@GetMapping("/api/places/{placeId}/visits")
	public ApiResponse<List<VisitResponse>> visits(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long placeId) {
		return ApiResponse.ok(visitService.byPlace(principal.getUserId(), placeId));
	}
}

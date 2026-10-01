package com.umatgong.domain.club.controller;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umatgong.domain.club.dto.ClubMemberResponse;
import com.umatgong.domain.club.dto.ClubResponse;
import com.umatgong.domain.club.dto.CreateClubRequest;
import com.umatgong.domain.club.dto.InviteRequest;
import com.umatgong.domain.club.dto.InviteResponse;
import com.umatgong.domain.club.dto.JoinClubRequest;
import com.umatgong.domain.club.dto.UpdateClubRequest;
import com.umatgong.domain.club.service.ClubService;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

	private final ClubService clubService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ClubResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody CreateClubRequest request) {
		return ApiResponse.ok(clubService.create(principal.getUserId(), request.name()));
	}

	@GetMapping
	public ApiResponse<List<ClubResponse>> myClubs(@AuthenticationPrincipal CustomUserDetails principal) {
		return ApiResponse.ok(clubService.myClubs(principal.getUserId()));
	}

	@PatchMapping("/{clubId}")
	public ApiResponse<ClubResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long clubId, @Valid @RequestBody UpdateClubRequest request) {
		return ApiResponse.ok(clubService.changeColor(principal.getUserId(), clubId, request.color()));
	}

	// 본문은 비워도 된다 ({"reissue": true}일 때만 새 코드)
	@PostMapping("/{clubId}/invite")
	public ApiResponse<InviteResponse> invite(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long clubId, @RequestBody(required = false) InviteRequest request) {
		boolean reissue = request != null && request.wantsReissue();
		return ApiResponse.ok(clubService.invite(principal.getUserId(), clubId, reissue));
	}

	@PostMapping("/join")
	public ApiResponse<ClubResponse> join(@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody JoinClubRequest request) {
		return ApiResponse.ok(clubService.join(principal.getUserId(), request.inviteCode()));
	}

	@GetMapping("/{clubId}/members")
	public ApiResponse<List<ClubMemberResponse>> members(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long clubId) {
		return ApiResponse.ok(clubService.members(principal.getUserId(), clubId));
	}

	@DeleteMapping("/{clubId}/members/me")
	public ApiResponse<Void> leave(@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable Long clubId) {
		clubService.leave(principal.getUserId(), clubId);
		return ApiResponse.ok();
	}
}

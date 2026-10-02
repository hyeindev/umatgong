package com.umatgong.domain.photo.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.umatgong.domain.photo.dto.ThumbnailResponse;
import com.umatgong.domain.photo.service.PhotoService;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

	private final PhotoService photoService;

	@PostMapping(path = "/thumbnails", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ThumbnailResponse> upload(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestPart("file") MultipartFile file) throws IOException {
		return ApiResponse.ok(photoService.uploadThumbnail(principal.getUserId(), file.getBytes()));
	}
}

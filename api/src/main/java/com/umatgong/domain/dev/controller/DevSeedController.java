package com.umatgong.domain.dev.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.umatgong.domain.dev.dto.DevSeedResponse;
import com.umatgong.domain.dev.service.DevSeedProperties;
import com.umatgong.domain.dev.service.DevSeedService;
import com.umatgong.global.response.ApiResponse;
import com.umatgong.global.security.CustomUserDetails;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 개발 전용 시드 API. <b>운영에서는 켜지 않는다.</b>
 *
 * <p>존재 자체를 숨긴다. 플래그(APP_DEV_SEED_ENABLED)가 꺼져 있으면 이 컨트롤러가 등록되지 않아 없는 경로가 되고,
 * 켜져 있어도 헤더 토큰이 틀리면 없는 경로와 <b>똑같은</b> 404를 돌려준다 (403이면 있다는 사실이 드러난다).
 * 같은 이유로 POST 밖의 메서드도 405가 아니라 404다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "umatgong.dev.seed", name = "enabled", havingValue = "true")
public class DevSeedController {

	public static final String PATH = "/api/dev/seed";
	public static final String TOKEN_HEADER = "X-Dev-Seed-Token";

	private final DevSeedService devSeedService;
	private final DevSeedProperties properties;

	@PostConstruct
	void warnEnabled() {
		if (properties.hasUsableToken()) {
			log.warn("Dev seed endpoint {} is ENABLED. Never turn this on in production.", PATH);
		} else {
			log.warn("Dev seed is enabled but APP_DEV_SEED_TOKEN is missing or shorter than {} chars; "
				+ "{} will answer 404 to every request.", DevSeedProperties.MIN_TOKEN_LENGTH, PATH);
		}
	}

	// 메서드를 POST로 좁히지 않는다. 좁히면 GET 등에 405가 나가 경로가 있다는 것이 드러난다.
	@RequestMapping(PATH)
	public ApiResponse<DevSeedResponse> seed(@AuthenticationPrincipal CustomUserDetails principal,
		@RequestHeader(name = TOKEN_HEADER, required = false) String token, HttpServletRequest request)
		throws NoResourceFoundException {
		if (!HttpMethod.POST.matches(request.getMethod()) || !properties.accepts(token)) {
			// 없는 경로일 때 Spring이 던지는 것과 같은 예외. GlobalExceptionHandler가 같은 404 본문으로 바꾼다.
			throw new NoResourceFoundException(HttpMethod.valueOf(request.getMethod()), PATH.substring(1));
		}
		return ApiResponse.ok(devSeedService.seed(principal.getUserId()));
	}
}

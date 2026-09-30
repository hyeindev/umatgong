package com.umatgong.global.kakao;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 로그인 토큰 검증 전담. 장소·경로(Local API)는 KakaoLocalClient가 맡고,
 * 이 클래스는 사용자 API(kapi.kakao.com)만 부른다.
 *
 * <p>여기서 쓰는 토큰은 사용자의 카카오 액세스 토큰이다. REST API 키는 쓰지 않는다.
 */
@Slf4j
@Component
public class KakaoAuthClient {

	private final RestClient restClient;
	private final long appId;

	public KakaoAuthClient(@Qualifier("kakaoApiRestClient") RestClient restClient, KakaoProperties properties) {
		// 앱 ID 없이 뜨면 다른 앱에서 발급된 카카오 토큰도 로그인에 통과한다. 기동 단계에서 막는다.
		this.appId = Objects.requireNonNull(properties.appId(),
			"umatgong.kakao.app-id (KAKAO_APP_ID) must be set");
		this.restClient = restClient;
	}

	/**
	 * 토큰이 우리 앱에서 발급됐는지 확인한 뒤 사용자 정보를 가져온다.
	 *
	 * @throws BusinessException KAKAO_INVALID_TOKEN — 토큰이 무효이거나 다른 앱의 토큰
	 * @throws BusinessException KAKAO_UNAVAILABLE — 카카오 장애·타임아웃
	 */
	public KakaoUserInfo verifyAndFetchUser(String kakaoAccessToken) {
		TokenInfoResponse tokenInfo = call("GET /v1/user/access_token_info", () -> restClient.get()
			.uri("/v1/user/access_token_info")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
			.retrieve()
			.body(TokenInfoResponse.class));
		if (tokenInfo == null || tokenInfo.appId() == null || tokenInfo.appId() != appId) {
			log.warn("Kakao token issued for another app: appId={}", tokenInfo == null ? null : tokenInfo.appId());
			throw new BusinessException(ErrorCode.KAKAO_INVALID_TOKEN);
		}

		UserMeResponse me = call("GET /v2/user/me", () -> restClient.get()
			.uri("/v2/user/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
			.retrieve()
			.body(UserMeResponse.class));
		if (me == null || me.id() == null) {
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE);
		}
		Profile profile = me.kakaoAccount() == null ? null : me.kakaoAccount().profile();
		if (profile == null) {
			return new KakaoUserInfo(me.id(), null, null);
		}
		// 기본 이미지는 "사진 없음"으로 본다. 카카오 회색 실루엣을 아바타로 쓰지 않는다.
		String image = Boolean.TRUE.equals(profile.isDefaultImage()) ? null : profile.profileImageUrl();
		return new KakaoUserInfo(me.id(), profile.nickname(), image);
	}

	// 호출 건수가 곧 비용이므로 모든 카카오 호출을 같은 형식으로 남긴다. 토큰은 로그에 쓰지 않는다.
	private <T> T call(String endpoint, KakaoCall<T> call) {
		long start = System.nanoTime();
		try {
			T result = call.execute();
			log.info("kakao call {} -> ok ({}ms)", endpoint, elapsedMillis(start));
			return result;
		} catch (HttpClientErrorException e) {
			log.info("kakao call {} -> {} ({}ms)", endpoint, e.getStatusCode().value(), elapsedMillis(start));
			if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
				|| e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
				throw new BusinessException(ErrorCode.KAKAO_INVALID_TOKEN);
			}
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE);
		} catch (RestClientException e) {
			log.warn("kakao call {} -> failed ({}ms): {}", endpoint, elapsedMillis(start), e.getMessage());
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE);
		}
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	@FunctionalInterface
	private interface KakaoCall<T> {
		T execute();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record TokenInfoResponse(Long id, @JsonProperty("app_id") Long appId) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record UserMeResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record KakaoAccount(Profile profile) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Profile(
		String nickname,
		@JsonProperty("profile_image_url") String profileImageUrl,
		@JsonProperty("is_default_image") Boolean isDefaultImage) {
	}
}

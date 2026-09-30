package com.umatgong.global.kakao;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 로그인 전담. 장소·경로(Local API)는 KakaoLocalClient가 맡는다.
 *
 * <ul>
 *   <li>사용자 API(kapi.kakao.com): 사용자의 카카오 액세스 토큰으로 검증·조회한다. REST API 키는 싣지 않는다</li>
 *   <li>인증 서버(kauth.kakao.com): 웹 로그인의 인가 코드를 액세스 토큰으로 바꾼다.
 *       REST API 키를 요청 본문에만 싣는다. 키는 로그·응답에 남기지 않는다</li>
 * </ul>
 */
@Slf4j
@Component
public class KakaoAuthClient {

	private static final String TOKEN_PATH = "/oauth/token";

	private final RestClient restClient;
	private final RestClient oauthRestClient;
	private final long appId;
	private final String restApiKey;
	private final String clientSecret;
	private final List<String> allowedRedirectUris;

	public KakaoAuthClient(@Qualifier("kakaoApiRestClient") RestClient restClient,
		@Qualifier("kakaoOAuthRestClient") RestClient oauthRestClient, KakaoProperties properties) {
		// 앱 ID 없이 뜨면 다른 앱에서 발급된 카카오 토큰도 로그인에 통과한다. 기동 단계에서 막는다.
		this.appId = Objects.requireNonNull(properties.appId(),
			"umatgong.kakao.app-id (KAKAO_APP_ID) must be set");
		String key = properties.restApiKey();
		if (key == null || key.isBlank()) {
			throw new IllegalStateException("umatgong.kakao.rest-api-key (KAKAO_REST_API_KEY) must be set");
		}
		this.restApiKey = key;
		this.clientSecret = properties.clientSecret();
		this.allowedRedirectUris = properties.allowedRedirectUris();
		this.restClient = restClient;
		this.oauthRestClient = oauthRestClient;
	}

	/**
	 * 웹 로그인의 인가 코드를 카카오 액세스 토큰으로 바꾼다.
	 *
	 * <p>redirectUri는 클라이언트가 보낸 값을 그대로 카카오에 넘긴다. 카카오는 authorize 때와 같은 값인지만 보므로,
	 * 우리가 허용한 주소가 아니면 카카오를 부르기 전에 거부한다 (오픈 리다이렉트 방지). 비교는 문자열 완전 일치다.
	 *
	 * @throws BusinessException KAKAO_REDIRECT_URI_NOT_ALLOWED — 허용 목록에 없는 redirectUri
	 * @throws BusinessException KAKAO_INVALID_TOKEN — 코드 만료·재사용, redirectUri 불일치
	 * @throws BusinessException KAKAO_UNAVAILABLE — 카카오 장애·타임아웃
	 * @throws BusinessException INTERNAL_ERROR — 카카오가 우리 키를 거부 (서버 설정 오류)
	 */
	public String exchangeCode(String code, String redirectUri) {
		if (!allowedRedirectUris.contains(redirectUri)) {
			// 클라이언트가 보낸 값이므로 줄바꿈을 지워 로그 한 줄을 위조하지 못하게 한다
			log.warn("Rejected Kakao redirectUri not in allow-list: {}",
				redirectUri == null ? null : redirectUri.replaceAll("[\\r\\n]", "_"));
			throw new BusinessException(ErrorCode.KAKAO_REDIRECT_URI_NOT_ALLOWED);
		}

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", restApiKey);
		form.add("redirect_uri", redirectUri);
		form.add("code", code);
		if (clientSecret != null) {
			form.add("client_secret", clientSecret);
		}

		OAuthTokenResponse token = call("POST " + TOKEN_PATH, () -> oauthRestClient.post()
			.uri(TOKEN_PATH)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(OAuthTokenResponse.class), KakaoAuthClient::oauthErrorCode);
		if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
			throw new BusinessException(ErrorCode.KAKAO_UNAVAILABLE);
		}
		return token.accessToken();
	}

	// 인증 서버의 4xx는 본문의 error로 원인을 가른다. 우리 키가 거부된 경우는 사용자가 다시 해도 소용없다.
	private static ErrorCode oauthErrorCode(HttpClientErrorException e) {
		OAuthErrorResponse error;
		try {
			error = e.getResponseBodyAs(OAuthErrorResponse.class);
		} catch (RuntimeException parseFailure) {
			error = null;
		}
		String type = error == null ? null : error.error();
		if ("invalid_client".equals(type) || "unauthorized_client".equals(type)) {
			log.error("Kakao rejected our client credentials (check KAKAO_REST_API_KEY / KAKAO_CLIENT_SECRET): {}",
				error.errorCode());
			return ErrorCode.INTERNAL_ERROR;
		}
		log.info("Kakao token exchange rejected: error={}, errorCode={}", type,
			error == null ? null : error.errorCode());
		return ErrorCode.KAKAO_INVALID_TOKEN;
	}

	private static ErrorCode userApiErrorCode(HttpClientErrorException e) {
		if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
			|| e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
			return ErrorCode.KAKAO_INVALID_TOKEN;
		}
		return ErrorCode.KAKAO_UNAVAILABLE;
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
			.body(TokenInfoResponse.class), KakaoAuthClient::userApiErrorCode);
		if (tokenInfo == null || tokenInfo.appId() == null || tokenInfo.appId() != appId) {
			log.warn("Kakao token issued for another app: appId={}", tokenInfo == null ? null : tokenInfo.appId());
			throw new BusinessException(ErrorCode.KAKAO_INVALID_TOKEN);
		}

		UserMeResponse me = call("GET /v2/user/me", () -> restClient.get()
			.uri("/v2/user/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
			.retrieve()
			.body(UserMeResponse.class), KakaoAuthClient::userApiErrorCode);
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

	// 호출 건수가 곧 비용이므로 모든 카카오 호출을 같은 형식으로 남긴다. 토큰·코드·키는 로그에 쓰지 않는다.
	// 4xx는 호출마다 뜻이 달라 clientErrorCode가 가른다. 5xx·타임아웃은 모두 카카오 장애다.
	private <T> T call(String endpoint, KakaoCall<T> call,
		Function<HttpClientErrorException, ErrorCode> clientErrorCode) {
		long start = System.nanoTime();
		try {
			T result = call.execute();
			log.info("kakao call {} -> ok ({}ms)", endpoint, elapsedMillis(start));
			return result;
		} catch (HttpClientErrorException e) {
			log.info("kakao call {} -> {} ({}ms)", endpoint, e.getStatusCode().value(), elapsedMillis(start));
			throw new BusinessException(clientErrorCode.apply(e));
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
	record OAuthTokenResponse(@JsonProperty("access_token") String accessToken) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record OAuthErrorResponse(String error, @JsonProperty("error_code") String errorCode) {
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

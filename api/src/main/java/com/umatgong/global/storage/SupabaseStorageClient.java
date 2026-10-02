package com.umatgong.global.storage;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.umatgong.global.error.BusinessException;
import com.umatgong.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * Supabase Storage 호출 전담. service_role 키는 이 클래스만 쓴다.
 * https://supabase.com/docs/reference/api (Storage: object upload / delete)
 */
@Slf4j
@Component
public class SupabaseStorageClient {

	private final PhotoStorageProperties properties;
	private final RestClient restClient;

	public SupabaseStorageClient(PhotoStorageProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(3))
			.withReadTimeout(Duration.ofSeconds(10));
		this.restClient = builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings)).build();
	}

	/** 새 파일로 올린다. 같은 경로가 있으면 덮어쓰지 않고 실패한다 */
	public void upload(String path, byte[] bytes, MediaType contentType) {
		requireConfigured();
		try {
			restClient.post()
				.uri(properties.base() + "/storage/v1/object/" + properties.bucket() + "/" + path)
				.headers(this::auth)
				.header("x-upsert", "false")
				.contentType(contentType)
				.body(bytes)
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientException e) {
			// 키·버킷 설정 문제이거나 저장소 장애. 사용자가 고칠 수 없다
			log.warn("photo storage upload failed: {}", e.getMessage());
			throw new BusinessException(ErrorCode.PHOTO_STORAGE_UNAVAILABLE);
		}
	}

	/** 지운다. 실패해도 기록 삭제는 되돌리지 않으므로 예외를 던지지 않고 로그만 남긴다 */
	public void deleteQuietly(List<String> paths) {
		if (paths.isEmpty() || !properties.configured()) {
			return;
		}
		try {
			restClient.method(org.springframework.http.HttpMethod.DELETE)
				.uri(properties.base() + "/storage/v1/object/" + properties.bucket())
				.headers(this::auth)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("prefixes", paths))
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientException e) {
			log.warn("photo storage delete failed for {} objects: {}", paths.size(), e.getMessage());
		}
	}

	private void auth(HttpHeaders headers) {
		headers.setBearerAuth(properties.serviceKey());
		headers.set("apikey", properties.serviceKey());
	}

	private void requireConfigured() {
		if (!properties.configured()) {
			throw new BusinessException(ErrorCode.PHOTO_STORAGE_UNAVAILABLE);
		}
	}
}

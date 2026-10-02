package com.umatgong.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 썸네일 저장소 (Supabase Storage). 키는 환경변수로만 받는다.
 *
 * @param supabaseUrl  프로젝트 주소. 예: https://abcd.supabase.co. 비어 있으면 사진 업로드를 받지 않는다
 * @param serviceKey   service_role 키. 서버에만 있다. 프론트로 내려보내지 않는다
 * @param bucket       공개(public) 버킷 이름
 * @param maxBytes     썸네일 한 장의 최대 크기
 * @param maxDimension 썸네일 긴 변의 최대 픽셀. 이보다 크면 원본으로 보고 받지 않는다
 */
@ConfigurationProperties(prefix = "umatgong.photo")
public record PhotoStorageProperties(String supabaseUrl, String serviceKey, String bucket, int maxBytes,
	int maxDimension) {

	public boolean configured() {
		return supabaseUrl != null && !supabaseUrl.isBlank() && serviceKey != null && !serviceKey.isBlank()
			&& bucket != null && !bucket.isBlank();
	}

	/** 공개 주소. 버킷의 이 경로 아래 파일은 이 주소로 누구나 읽을 수 있다 */
	public String publicUrl(String path) {
		return base() + "/storage/v1/object/public/" + bucket + "/" + path;
	}

	public String base() {
		return supabaseUrl == null ? "" : supabaseUrl.replaceAll("/+$", "");
	}
}

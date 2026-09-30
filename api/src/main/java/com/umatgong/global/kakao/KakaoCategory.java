package com.umatgong.global.kakao;

/** 카카오 로컬 API의 카테고리 그룹 코드. 이 서비스는 음식점과 카페만 다룬다. */
public enum KakaoCategory {

	RESTAURANT("FD6"),
	CAFE("CE7");

	private final String code;

	KakaoCategory(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static boolean isSupported(String code) {
		for (KakaoCategory category : values()) {
			if (category.code.equals(code)) {
				return true;
			}
		}
		return false;
	}
}

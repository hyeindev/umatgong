package com.umatgong.domain.place.entity;

public enum PlaceSource {
	/** 카카오 장소 검색 결과를 캐싱한 장소. 모든 클럽이 공유한다. */
	API,
	/** 사용자가 직접 입력한 장소. 만든 클럽 안에서만 후보로 노출한다. */
	USER
}

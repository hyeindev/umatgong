package com.umatgong.domain.visit.entity;

public enum TagSource {
	/** iOS Vision / Android ML Kit. 기본 경로 */
	ON_DEVICE,
	/** 서버 분석 (기획서 10.1 대비책). 온보딩 대량 스캔에는 쓰지 않는다. */
	SERVER
}

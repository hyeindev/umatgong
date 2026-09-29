/**
 * 카카오 REST API 호출 전담. 다른 패키지에서 카카오를 직접 호출하지 않는다.
 *
 * <p>카카오는 x가 경도, y가 위도다. 이 변환은 KakaoLocalClient 안에서만 일어나고,
 * 밖으로는 항상 lat/lng로 내보낸다. REST API 키는 이 서버에만 존재한다.
 */
package com.umatgong.global.kakao;

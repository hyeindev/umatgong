package com.umatgong.global.kakao;

/**
 * 카카오에서 받은 사용자 정보. 동의항목은 닉네임과 프로필 이미지뿐이라 이메일은 없다.
 *
 * @param kakaoId     카카오 회원번호. 우리 서비스의 사용자 식별 기준
 * @param nickname    동의를 안 했으면 null
 * @param profileImageUrl 동의를 안 했거나 기본 이미지면 null
 */
public record KakaoUserInfo(long kakaoId, String nickname, String profileImageUrl) {
}

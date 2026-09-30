package com.umatgong.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.umatgong.domain.user.entity.User;

/**
 * @param newUser 이번 로그인으로 가입했으면 true. 프론트는 이 값으로 첫 실행 온보딩(S1)을 띄운다.
 */
public record LoginResponse(@JsonUnwrapped TokenResponse tokens, UserSummary user, boolean newUser) {

	public record UserSummary(Long id, String name, String avatarUrl) {

		public static UserSummary from(User user) {
			return new UserSummary(user.getId(), user.getName(), user.getAvatarUrl());
		}
	}
}

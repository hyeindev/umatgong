package com.umatgong.global.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 토큰에서 꺼낸 사용자 ID만 담는다. 요청마다 DB에서 사용자를 다시 읽지 않기 위해서다.
 * 클럽 멤버십 같은 권한 판단은 여기서 하지 않고 서비스 레이어가 한다.
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

	private final Long userId;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of();
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return String.valueOf(userId);
	}
}

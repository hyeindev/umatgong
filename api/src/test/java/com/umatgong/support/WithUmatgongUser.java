package com.umatgong.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.umatgong.global.security.CustomUserDetails;

/** 슬라이스 테스트용 로그인 사용자. 실제 필터처럼 principal에 CustomUserDetails를 넣는다 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithUmatgongUser.Factory.class)
public @interface WithUmatgongUser {

	long userId() default 1L;

	class Factory implements WithSecurityContextFactory<WithUmatgongUser> {
		@Override
		public SecurityContext createSecurityContext(WithUmatgongUser annotation) {
			CustomUserDetails principal = new CustomUserDetails(annotation.userId());
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
			return context;
		}
	}
}

package com.umatgong.domain.club.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * 초대 코드. 192비트 무작위 값을 URL에 그대로 쓸 수 있는 32자(base64url)로 만든다.
 * 순번이나 짧은 숫자는 대입으로 남의 클럽에 들어갈 수 있으므로 쓰지 않는다.
 */
@Component
public class InviteCodeGenerator {

	private static final int BYTES = 24;
	private static final SecureRandom RANDOM = new SecureRandom();

	public String next() {
		byte[] bytes = new byte[BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}

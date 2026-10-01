package com.umatgong.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class InviteCodeGeneratorTest {

	private final InviteCodeGenerator generator = new InviteCodeGenerator();

	@Test
	void 코드는_URL에_그대로_쓸_수_있는_32자다() {
		String code = generator.next();

		// clubs.invite_code는 varchar(32)
		assertThat(code).hasSize(32).matches("[A-Za-z0-9_-]{32}");
	}

	@Test
	void 순번이_아니라_매번_무작위다() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			codes.add(generator.next());
		}

		assertThat(codes).hasSize(1000);
		assertThat(codes).noneMatch(code -> code.matches("\\d+"));
	}
}

package com.umatgong.domain.dev.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DevSeedPropertiesTest {

	private static final String TOKEN = "a".repeat(DevSeedProperties.MIN_TOKEN_LENGTH);

	@Test
	void 켜져_있고_토큰이_같을_때만_받는다() {
		assertThat(new DevSeedProperties(true, TOKEN).accepts(TOKEN)).isTrue();
	}

	@Test
	void 꺼져_있으면_토큰이_맞아도_받지_않는다() {
		assertThat(new DevSeedProperties(false, TOKEN).accepts(TOKEN)).isFalse();
	}

	@Test
	void 헤더가_없거나_다르면_받지_않는다() {
		DevSeedProperties properties = new DevSeedProperties(true, TOKEN);

		assertThat(properties.accepts(null)).isFalse();
		assertThat(properties.accepts("")).isFalse();
		assertThat(properties.accepts(TOKEN + "x")).isFalse();
		assertThat(properties.accepts(TOKEN.substring(1))).isFalse();
	}

	@Test
	void 서버_토큰이_비었거나_짧으면_어떤_값도_받지_않는다() {
		assertThat(new DevSeedProperties(true, null).accepts(null)).isFalse();
		assertThat(new DevSeedProperties(true, "").accepts("")).isFalse();
		assertThat(new DevSeedProperties(true, "   ").accepts("   ")).isFalse();
		String shortToken = "a".repeat(DevSeedProperties.MIN_TOKEN_LENGTH - 1);
		assertThat(new DevSeedProperties(true, shortToken).accepts(shortToken)).isFalse();
	}
}

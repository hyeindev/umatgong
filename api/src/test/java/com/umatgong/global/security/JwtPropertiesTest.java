package com.umatgong.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class JwtPropertiesTest {

	@Test
	void 단위_없는_만료값은_초로_읽는다() {
		Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
			"umatgong.jwt.secret", "x",
			"umatgong.jwt.access-token-validity", "1800",
			"umatgong.jwt.refresh-token-validity", "1209600")));

		JwtProperties props = binder.bind("umatgong.jwt", JwtProperties.class).get();

		assertThat(props.accessTokenValidity()).isEqualTo(Duration.ofMinutes(30));
		assertThat(props.refreshTokenValidity()).isEqualTo(Duration.ofDays(14));
	}
}

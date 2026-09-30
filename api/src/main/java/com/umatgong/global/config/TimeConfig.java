package com.umatgong.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 토큰 만료처럼 시간에 기대는 로직을 테스트에서 고정된 시각으로 돌리기 위해 Clock을 주입받는다.
@Configuration
public class TimeConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}

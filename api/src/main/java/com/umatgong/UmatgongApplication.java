package com.umatgong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// 인증은 자체 JWT만 쓴다. 기본 인메모리 사용자(랜덤 비밀번호)가 생기지 않도록 제외한다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class UmatgongApplication {

	public static void main(String[] args) {
		SpringApplication.run(UmatgongApplication.class, args);
	}
}

package com.umatgong.global.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "umatgong.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity) {
}

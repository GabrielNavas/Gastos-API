package com.navas.gastosapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long accessTtlMinutes, long refreshTtlDays) {
}

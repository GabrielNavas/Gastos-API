package com.navas.gastosapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(int generalPerMinute, int authPerMinute) {
}

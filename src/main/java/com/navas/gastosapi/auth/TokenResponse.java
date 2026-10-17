package com.navas.gastosapi.auth;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}

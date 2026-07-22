package com.atharva.dealership.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String role) {

    public AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
        this(accessToken, refreshToken, tokenType, expiresInSeconds, "CUSTOMER");
    }
}

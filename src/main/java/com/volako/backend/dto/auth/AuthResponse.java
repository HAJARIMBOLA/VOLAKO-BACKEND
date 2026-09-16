package com.volako.backend.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

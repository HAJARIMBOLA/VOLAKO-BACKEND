package com.volako.backend.dto.auth;

import com.volako.backend.domain.User;

public record UserResponse(
        Long id,
        String phoneNumber,
        String fullName
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getPhoneNumber(), user.getFullName());
    }
}

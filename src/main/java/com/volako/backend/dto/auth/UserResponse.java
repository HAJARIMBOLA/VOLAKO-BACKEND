package com.volako.backend.dto.auth;

import com.volako.backend.domain.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String phoneNumber,
        String email
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getPhoneNumber(), user.getEmail());
    }
}

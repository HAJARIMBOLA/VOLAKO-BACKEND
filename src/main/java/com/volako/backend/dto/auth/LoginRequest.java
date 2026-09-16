package com.volako.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'email est requis") String email,
        @NotBlank(message = "Le mot de passe est requis") String password
) {
}

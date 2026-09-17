package com.volako.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Le numéro de téléphone est requis") String phoneNumber,
        @NotBlank(message = "Le mot de passe est requis") String password
) {
}

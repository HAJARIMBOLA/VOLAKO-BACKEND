package com.volako.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** identifier accepts either the account's email or its phone number. */
public record LoginRequest(
        @NotBlank(message = "L'email ou le numéro de téléphone est requis") String identifier,
        @NotBlank(message = "Le mot de passe est requis") String password
) {
}

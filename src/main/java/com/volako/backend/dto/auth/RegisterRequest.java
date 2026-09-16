package com.volako.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "L'email est requis") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Le mot de passe est requis") @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
        @NotBlank(message = "Le nom complet est requis") String fullName
) {
}

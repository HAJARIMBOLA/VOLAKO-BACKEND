package com.volako.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le numéro de téléphone est requis")
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Numéro de téléphone invalide")
        String phoneNumber,
        @NotBlank(message = "Le mot de passe est requis") @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
        @NotBlank(message = "Le nom complet est requis") String fullName
) {
}

package com.volako.backend.dto.account;

import com.volako.backend.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(
        @NotBlank(message = "Le nom du compte est requis") String name,
        @NotNull(message = "Le type de compte est requis") AccountType type,
        Boolean allowNegativeBalance
) {
}

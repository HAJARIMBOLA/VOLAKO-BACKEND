package com.volako.backend.dto.category;

import com.volako.backend.domain.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank(message = "Le nom de la catégorie est requis") String name,
        @NotNull(message = "Le type de catégorie est requis") TransactionType type
) {
}

package com.volako.backend.dto.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotBlank(message = "Le nom du fonds est requis") String name,
        @NotNull(message = "Le montant cible est requis") @DecimalMin(value = "0.01", message = "Le montant cible doit être positif") BigDecimal targetAmount,
        LocalDate targetDate
) {
}

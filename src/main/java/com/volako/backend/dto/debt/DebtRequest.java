package com.volako.backend.dto.debt;

import com.volako.backend.domain.enums.DebtDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtRequest(
        @NotNull(message = "Le sens de la dette est requis") DebtDirection direction,
        @NotBlank(message = "Le nom de la personne est requis") String personName,
        @NotNull(message = "Le montant est requis") @DecimalMin(value = "0.01", message = "Le montant doit être positif") BigDecimal amount,
        @Size(max = 255, message = "La description est trop longue") String description,
        LocalDate dueDate
) {
}

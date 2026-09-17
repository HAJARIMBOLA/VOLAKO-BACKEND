package com.volako.backend.dto.debt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtPaymentRequest(
        @NotNull(message = "Le compte est requis") Long accountId,
        @NotNull(message = "Le montant est requis") @DecimalMin(value = "0.01", message = "Le montant doit être positif") BigDecimal amount,
        @NotNull(message = "La date est requise") LocalDate paymentDate
) {
}

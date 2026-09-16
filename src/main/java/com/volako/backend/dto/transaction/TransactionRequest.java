package com.volako.backend.dto.transaction;

import com.volako.backend.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "Le type est requis") TransactionType type,
        @NotNull(message = "Le montant est requis") @DecimalMin(value = "0.01", message = "Le montant doit être positif") BigDecimal amount,
        @NotNull(message = "Le compte est requis") Long accountId,
        @NotNull(message = "La catégorie est requise") Long categoryId,
        @Size(max = 255, message = "La description est trop longue") String description,
        @NotNull(message = "La date est requise") LocalDate transactionDate
) {
}

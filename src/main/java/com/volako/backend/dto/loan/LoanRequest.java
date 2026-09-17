package com.volako.backend.dto.loan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanRequest(
        @NotBlank(message = "Le nom du crédit est requis") String name,
        @NotNull(message = "Le montant du principal est requis") @DecimalMin(value = "0.01", message = "Le principal doit être positif") BigDecimal principalAmount,
        @NotNull(message = "La mensualité est requise") @DecimalMin(value = "0.01", message = "La mensualité doit être positive") BigDecimal monthlyPayment,
        @NotNull(message = "La durée est requise") @Min(value = 1, message = "La durée doit être d'au moins 1 mois") Integer durationMonths,
        @NotNull(message = "La date de début est requise") LocalDate startDate
) {
}

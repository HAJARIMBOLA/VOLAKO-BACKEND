package com.volako.backend.dto.debt;

import com.volako.backend.domain.Debt;
import com.volako.backend.domain.enums.DebtDirection;
import com.volako.backend.domain.enums.DebtStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtResponse(
        Long id,
        DebtDirection direction,
        String personName,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String description,
        LocalDate dueDate,
        DebtStatus status,
        boolean overdue
) {
    public static DebtResponse from(Debt debt, BigDecimal paidAmount) {
        BigDecimal remaining = debt.getAmount().subtract(paidAmount);
        boolean overdue = debt.getStatus() != DebtStatus.PAID
                && debt.getDueDate() != null
                && debt.getDueDate().isBefore(LocalDate.now());

        return new DebtResponse(
                debt.getId(),
                debt.getDirection(),
                debt.getPersonName(),
                debt.getAmount(),
                paidAmount,
                remaining,
                debt.getDescription(),
                debt.getDueDate(),
                debt.getStatus(),
                overdue
        );
    }
}

package com.volako.backend.dto.loan;

import com.volako.backend.domain.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        String name,
        BigDecimal principalAmount,
        BigDecimal monthlyPayment,
        Integer durationMonths,
        LocalDate startDate,
        int paidInstallments,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        boolean completed
) {
    public static LoanResponse from(Loan loan, long paidInstallments, BigDecimal paidAmount) {
        BigDecimal remaining = loan.getPrincipalAmount().subtract(paidAmount);
        boolean completed = remaining.compareTo(BigDecimal.ZERO) <= 0;

        return new LoanResponse(
                loan.getId(),
                loan.getName(),
                loan.getPrincipalAmount(),
                loan.getMonthlyPayment(),
                loan.getDurationMonths(),
                loan.getStartDate(),
                (int) paidInstallments,
                paidAmount,
                remaining.max(BigDecimal.ZERO),
                completed
        );
    }
}

package com.volako.backend.dto.loan;

import com.volako.backend.domain.LoanPayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanPaymentResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        Long accountId,
        String accountName
) {
    public static LoanPaymentResponse from(LoanPayment payment) {
        return new LoanPaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getAccount().getId(),
                payment.getAccount().getName()
        );
    }
}

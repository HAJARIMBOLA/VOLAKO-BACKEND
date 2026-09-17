package com.volako.backend.dto.debt;

import com.volako.backend.domain.DebtPayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtPaymentResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        Long accountId,
        String accountName
) {
    public static DebtPaymentResponse from(DebtPayment payment) {
        return new DebtPaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getAccount().getId(),
                payment.getAccount().getName()
        );
    }
}

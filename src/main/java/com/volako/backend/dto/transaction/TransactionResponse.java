package com.volako.backend.dto.transaction;

import com.volako.backend.domain.Transaction;
import com.volako.backend.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        String description,
        LocalDate transactionDate
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getDescription(),
                transaction.getTransactionDate()
        );
    }
}

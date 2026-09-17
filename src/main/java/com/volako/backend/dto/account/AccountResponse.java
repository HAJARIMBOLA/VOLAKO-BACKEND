package com.volako.backend.dto.account;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.enums.AccountType;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        String currency,
        boolean allowNegativeBalance,
        boolean active,
        BigDecimal balance,
        String accountNumber
) {
    public static AccountResponse from(Account account, BigDecimal balance) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.isAllowNegativeBalance(),
                account.isActive(),
                balance,
                account.getAccountNumber()
        );
    }
}

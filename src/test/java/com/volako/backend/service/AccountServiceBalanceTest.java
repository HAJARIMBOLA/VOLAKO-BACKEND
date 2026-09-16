package com.volako.backend.service;

import com.volako.backend.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceBalanceTest {

    @Mock
    private com.volako.backend.repository.AccountRepository accountRepository;
    @Mock
    private com.volako.backend.repository.TransactionRepository transactionRepository;
    @Mock
    private com.volako.backend.repository.UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void computesBalanceAsIncomeMinusExpense() {
        when(transactionRepository.sumAmountByAccountIdAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("500000"));
        when(transactionRepository.sumAmountByAccountIdAndType(1L, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("120000"));

        BigDecimal balance = accountService.computeBalance(1L);

        assertThat(balance).isEqualByComparingTo("380000");
    }

    @Test
    void balanceIsZeroWhenNoTransactions() {
        when(transactionRepository.sumAmountByAccountIdAndType(2L, TransactionType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumAmountByAccountIdAndType(2L, TransactionType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal balance = accountService.computeBalance(2L);

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

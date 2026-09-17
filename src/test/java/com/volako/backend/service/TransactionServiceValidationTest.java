package com.volako.backend.service;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.Category;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.AccountType;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.dto.transaction.TransactionRequest;
import com.volako.backend.exception.BusinessConflictException;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.CategoryRepository;
import com.volako.backend.repository.TransactionRepository;
import com.volako.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceValidationTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private final User user = User.builder().id(1L).phoneNumber("0340000000").build();
    private final Category expenseCategory = Category.builder().id(10L).type(TransactionType.EXPENSE).build();

    @Test
    void rejectsExpenseThatWouldMakeBalanceNegativeWhenNegativeBalanceIsDisallowed() {
        Account account = Account.builder().id(5L).allowNegativeBalance(false).build();
        when(accountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        lenient().when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(accountService.computeBalance(5L)).thenReturn(new BigDecimal("100000"));

        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("150000"), 5L, 10L, "Loyer", LocalDate.now());

        assertThatThrownBy(() -> transactionService.createTransaction(1L, request))
                .isInstanceOf(BusinessConflictException.class);

        verify(transactionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void allowsExpenseThatKeepsBalanceNonNegative() {
        Account account = Account.builder().id(5L).allowNegativeBalance(false).build();
        when(accountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(accountService.computeBalance(5L)).thenReturn(new BigDecimal("100000"));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("50000"), 5L, 10L, "Courses", LocalDate.now());

        var response = transactionService.createTransaction(1L, request);

        assertThat(response.amount()).isEqualByComparingTo("50000");
        verify(transactionRepository).save(any());
    }

    @Test
    void allowsNegativeBalanceWhenAccountPermitsIt() {
        Account account = Account.builder().id(5L).allowNegativeBalance(true).build();
        when(accountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(expenseCategory));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRequest request = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("500000"), 5L, 10L, "Urgence", LocalDate.now());

        var response = transactionService.createTransaction(1L, request);

        assertThat(response.amount()).isEqualByComparingTo("500000");
    }
}

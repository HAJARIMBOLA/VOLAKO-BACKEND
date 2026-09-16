package com.volako.backend.service;

import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.dto.dashboard.DashboardResponse;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, LocalDate from, LocalDate to) {
        LocalDate periodFrom = from != null ? from : YearMonth.now().atDay(1);
        LocalDate periodTo = to != null ? to : YearMonth.now().atEndOfMonth();

        BigDecimal totalBalance = accountRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(account -> accountService.computeBalance(account.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndPeriod(
                userId, TransactionType.INCOME, periodFrom, periodTo);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndTypeAndPeriod(
                userId, TransactionType.EXPENSE, periodFrom, periodTo);

        return new DashboardResponse(totalBalance, totalIncome, totalExpense, periodFrom, periodTo);
    }
}

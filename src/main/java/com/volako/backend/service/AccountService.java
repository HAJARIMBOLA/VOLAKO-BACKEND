package com.volako.backend.service;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.dto.account.AccountRequest;
import com.volako.backend.dto.account.AccountResponse;
import com.volako.backend.exception.ResourceNotFoundException;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.TransactionRepository;
import com.volako.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(Long userId) {
        return accountRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(account -> AccountResponse.from(account, computeBalance(account.getId())))
                .toList();
    }

    @Transactional
    public AccountResponse createAccount(Long userId, AccountRequest request) {
        User user = userRepository.getReferenceById(userId);

        Account account = Account.builder()
                .user(user)
                .name(request.name())
                .type(request.type())
                .allowNegativeBalance(request.allowNegativeBalance() == null || request.allowNegativeBalance())
                .accountNumber(request.accountNumber())
                .active(true)
                .build();
        account = accountRepository.save(account);

        return AccountResponse.from(account, BigDecimal.ZERO);
    }

    @Transactional
    public AccountResponse updateAccount(Long userId, Long accountId, AccountRequest request) {
        Account account = getOwnedAccount(userId, accountId);

        account.setName(request.name());
        account.setType(request.type());
        if (request.allowNegativeBalance() != null) {
            account.setAllowNegativeBalance(request.allowNegativeBalance());
        }
        account.setAccountNumber(request.accountNumber());
        account = accountRepository.save(account);

        return AccountResponse.from(account, computeBalance(account.getId()));
    }

    /** DELETE always archives; VOLAKO never physically deletes an account that could hold transaction history. */
    @Transactional
    public void archiveAccount(Long userId, Long accountId) {
        Account account = getOwnedAccount(userId, accountId);
        account.setActive(false);
        accountRepository.save(account);
    }

    public BigDecimal computeBalance(Long accountId) {
        BigDecimal income = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.INCOME);
        BigDecimal expense = transactionRepository.sumAmountByAccountIdAndType(accountId, TransactionType.EXPENSE);
        return income.subtract(expense);
    }

    private Account getOwnedAccount(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));
    }
}

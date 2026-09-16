package com.volako.backend.service;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.Category;
import com.volako.backend.domain.Transaction;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.dto.transaction.TransactionRequest;
import com.volako.backend.dto.transaction.TransactionResponse;
import com.volako.backend.exception.BusinessConflictException;
import com.volako.backend.exception.ResourceNotFoundException;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.CategoryRepository;
import com.volako.backend.repository.TransactionRepository;
import com.volako.backend.repository.TransactionSpecifications;
import com.volako.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(
            Long userId, Long accountId, Long categoryId, TransactionType type, LocalDate from, LocalDate to) {
        Specification<Transaction> spec = Specification
                .allOf(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.hasAccount(accountId))
                .and(TransactionSpecifications.hasCategory(categoryId))
                .and(TransactionSpecifications.hasType(type))
                .and(TransactionSpecifications.fromDate(from))
                .and(TransactionSpecifications.toDate(to));

        return transactionRepository.findAll(spec, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "transactionDate"))
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        Account account = getOwnedAccount(userId, request.accountId());
        Category category = getOwnedCategory(userId, request.categoryId());

        BigDecimal delta = signedDelta(request.type(), request.amount());
        assertBalanceAllowed(account, delta);

        User user = userRepository.getReferenceById(userId);
        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .type(request.type())
                .amount(request.amount())
                .description(request.description())
                .transactionDate(request.transactionDate())
                .build();
        transaction = transactionRepository.save(transaction);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = getOwnedTransaction(userId, transactionId);
        Account newAccount = getOwnedAccount(userId, request.accountId());
        Category newCategory = getOwnedCategory(userId, request.categoryId());

        BigDecimal oldDelta = signedDelta(transaction.getType(), transaction.getAmount());
        BigDecimal newDelta = signedDelta(request.type(), request.amount());

        if (transaction.getAccount().getId().equals(newAccount.getId())) {
            assertBalanceAllowed(newAccount, newDelta.subtract(oldDelta));
        } else {
            assertBalanceAllowed(newAccount, newDelta);
        }

        transaction.setAccount(newAccount);
        transaction.setCategory(newCategory);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction = transactionRepository.save(transaction);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = getOwnedTransaction(userId, transactionId);
        transactionRepository.delete(transaction);
    }

    private void assertBalanceAllowed(Account account, BigDecimal projectedDelta) {
        if (account.isAllowNegativeBalance()) {
            return;
        }
        BigDecimal projectedBalance = accountService.computeBalance(account.getId()).add(projectedDelta);
        if (projectedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessConflictException("INSUFFICIENT_BALANCE", "Solde insuffisant sur ce compte");
        }
    }

    private BigDecimal signedDelta(TransactionType type, BigDecimal amount) {
        return type == TransactionType.INCOME ? amount : amount.negate();
    }

    private Account getOwnedAccount(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
    }

    private Transaction getOwnedTransaction(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction introuvable"));
    }
}

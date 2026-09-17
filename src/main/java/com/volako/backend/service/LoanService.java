package com.volako.backend.service;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.Category;
import com.volako.backend.domain.Loan;
import com.volako.backend.domain.LoanPayment;
import com.volako.backend.domain.Transaction;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.dto.loan.LoanPaymentRequest;
import com.volako.backend.dto.loan.LoanPaymentResponse;
import com.volako.backend.dto.loan.LoanRequest;
import com.volako.backend.dto.loan.LoanResponse;
import com.volako.backend.exception.BusinessConflictException;
import com.volako.backend.exception.ResourceNotFoundException;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.CategoryRepository;
import com.volako.backend.repository.LoanPaymentRepository;
import com.volako.backend.repository.LoanRepository;
import com.volako.backend.repository.TransactionRepository;
import com.volako.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<LoanResponse> listLoans(Long userId) {
        return loanRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LoanResponse createLoan(Long userId, LoanRequest request) {
        User user = userRepository.getReferenceById(userId);
        Loan loan = Loan.builder()
                .user(user)
                .name(request.name())
                .principalAmount(request.principalAmount())
                .monthlyPayment(request.monthlyPayment())
                .durationMonths(request.durationMonths())
                .startDate(request.startDate())
                .build();
        loan = loanRepository.save(loan);

        return LoanResponse.from(loan, 0, BigDecimal.ZERO);
    }

    @Transactional
    public LoanResponse updateLoan(Long userId, Long loanId, LoanRequest request) {
        Loan loan = getOwnedLoan(userId, loanId);
        loan.setName(request.name());
        loan.setPrincipalAmount(request.principalAmount());
        loan.setMonthlyPayment(request.monthlyPayment());
        loan.setDurationMonths(request.durationMonths());
        loan.setStartDate(request.startDate());
        loan = loanRepository.save(loan);

        return toResponse(loan);
    }

    @Transactional
    public void deleteLoan(Long userId, Long loanId) {
        Loan loan = getOwnedLoan(userId, loanId);
        loanRepository.delete(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanPaymentResponse> listPayments(Long userId, Long loanId) {
        Loan loan = getOwnedLoan(userId, loanId);
        return loanPaymentRepository.findByLoanIdOrderByPaymentDateDesc(loan.getId()).stream()
                .map(LoanPaymentResponse::from)
                .toList();
    }

    /** Recording a payment moves real money, following the same pattern as debt repayments:
     *  it creates a real EXPENSE transaction on the chosen account, validated against
     *  allowNegativeBalance, so account balances and loan tracking never drift apart. */
    @Transactional
    public LoanPaymentResponse addPayment(Long userId, Long loanId, LoanPaymentRequest request) {
        Loan loan = getOwnedLoan(userId, loanId);
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        Category category = categoryRepository
                .findByUserIdAndNameAndType(userId, DefaultCategorySeeder.LOAN_REPAYMENT_CATEGORY, TransactionType.EXPENSE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie système introuvable: " + DefaultCategorySeeder.LOAN_REPAYMENT_CATEGORY));

        if (!account.isAllowNegativeBalance()) {
            BigDecimal projectedBalance = accountService.computeBalance(account.getId()).subtract(request.amount());
            if (projectedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessConflictException("INSUFFICIENT_BALANCE", "Solde insuffisant sur ce compte");
            }
        }

        Transaction transaction = Transaction.builder()
                .user(loan.getUser())
                .account(account)
                .category(category)
                .type(TransactionType.EXPENSE)
                .amount(request.amount())
                .description("Remboursement crédit — " + loan.getName())
                .transactionDate(request.paymentDate())
                .build();
        transactionRepository.save(transaction);

        LoanPayment payment = LoanPayment.builder()
                .loan(loan)
                .account(account)
                .amount(request.amount())
                .paymentDate(request.paymentDate())
                .build();
        payment = loanPaymentRepository.save(payment);

        return LoanPaymentResponse.from(payment);
    }

    private LoanResponse toResponse(Loan loan) {
        long paidInstallments = loanPaymentRepository.countByLoanId(loan.getId());
        BigDecimal paidAmount = loanPaymentRepository.sumAmountByLoanId(loan.getId());
        return LoanResponse.from(loan, paidInstallments, paidAmount);
    }

    private Loan getOwnedLoan(Long userId, Long loanId) {
        return loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit introuvable"));
    }
}

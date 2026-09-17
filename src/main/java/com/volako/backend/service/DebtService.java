package com.volako.backend.service;

import com.volako.backend.domain.Account;
import com.volako.backend.domain.Category;
import com.volako.backend.domain.Debt;
import com.volako.backend.domain.DebtPayment;
import com.volako.backend.domain.Transaction;
import com.volako.backend.domain.User;
import com.volako.backend.domain.enums.DebtDirection;
import com.volako.backend.domain.enums.DebtStatus;
import com.volako.backend.domain.enums.TransactionType;
import com.volako.backend.dto.debt.DebtPaymentRequest;
import com.volako.backend.dto.debt.DebtPaymentResponse;
import com.volako.backend.dto.debt.DebtRequest;
import com.volako.backend.dto.debt.DebtResponse;
import com.volako.backend.exception.BusinessConflictException;
import com.volako.backend.exception.ResourceNotFoundException;
import com.volako.backend.repository.AccountRepository;
import com.volako.backend.repository.CategoryRepository;
import com.volako.backend.repository.DebtPaymentRepository;
import com.volako.backend.repository.DebtRepository;
import com.volako.backend.repository.TransactionRepository;
import com.volako.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final DebtPaymentRepository debtPaymentRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<DebtResponse> listDebts(Long userId, DebtDirection direction) {
        List<Debt> debts = direction != null
                ? debtRepository.findByUserIdAndDirectionOrderByDueDateAscCreatedAtDesc(userId, direction)
                : debtRepository.findByUserIdOrderByDueDateAscCreatedAtDesc(userId);

        return debts.stream()
                .map(debt -> DebtResponse.from(debt, debtPaymentRepository.sumAmountByDebtId(debt.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DebtResponse> listOverdueDebts(Long userId) {
        return debtRepository.findOverdue(userId, LocalDate.now(), DebtStatus.PAID).stream()
                .map(debt -> DebtResponse.from(debt, debtPaymentRepository.sumAmountByDebtId(debt.getId())))
                .toList();
    }

    @Transactional
    public DebtResponse createDebt(Long userId, DebtRequest request) {
        User user = userRepository.getReferenceById(userId);
        Debt debt = Debt.builder()
                .user(user)
                .direction(request.direction())
                .personName(request.personName())
                .amount(request.amount())
                .description(request.description())
                .dueDate(request.dueDate())
                .status(DebtStatus.OPEN)
                .build();
        debt = debtRepository.save(debt);

        return DebtResponse.from(debt, BigDecimal.ZERO);
    }

    @Transactional
    public DebtResponse updateDebt(Long userId, Long debtId, DebtRequest request) {
        Debt debt = getOwnedDebt(userId, debtId);
        debt.setDirection(request.direction());
        debt.setPersonName(request.personName());
        debt.setAmount(request.amount());
        debt.setDescription(request.description());
        debt.setDueDate(request.dueDate());
        debt = debtRepository.save(debt);

        BigDecimal paid = debtPaymentRepository.sumAmountByDebtId(debt.getId());
        recomputeStatus(debt, paid);

        return DebtResponse.from(debt, paid);
    }

    @Transactional
    public void deleteDebt(Long userId, Long debtId) {
        Debt debt = getOwnedDebt(userId, debtId);
        debtRepository.delete(debt);
    }

    @Transactional(readOnly = true)
    public List<DebtPaymentResponse> listPayments(Long userId, Long debtId) {
        Debt debt = getOwnedDebt(userId, debtId);
        return debtPaymentRepository.findByDebtIdOrderByPaymentDateDesc(debt.getId()).stream()
                .map(DebtPaymentResponse::from)
                .toList();
    }

    /** Recording a payment also moves real money: a receivable payment is income on the
     *  chosen account, a payable payment is an expense from it — so account balances and
     *  debt tracking never drift apart. */
    @Transactional
    public DebtPaymentResponse addPayment(Long userId, Long debtId, DebtPaymentRequest request) {
        Debt debt = getOwnedDebt(userId, debtId);
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));

        boolean isReceivable = debt.getDirection() == DebtDirection.RECEIVABLE;
        TransactionType transactionType = isReceivable ? TransactionType.INCOME : TransactionType.EXPENSE;
        String categoryName = isReceivable
                ? DefaultCategorySeeder.DEBT_REPAYMENT_RECEIVED_CATEGORY
                : DefaultCategorySeeder.DEBT_REPAYMENT_MADE_CATEGORY;
        Category category = categoryRepository.findByUserIdAndNameAndType(userId, categoryName, transactionType)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie système introuvable: " + categoryName));

        if (!isReceivable && !account.isAllowNegativeBalance()) {
            BigDecimal projectedBalance = accountService.computeBalance(account.getId()).subtract(request.amount());
            if (projectedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessConflictException("INSUFFICIENT_BALANCE", "Solde insuffisant sur ce compte");
            }
        }

        Transaction transaction = Transaction.builder()
                .user(debt.getUser())
                .account(account)
                .category(category)
                .type(transactionType)
                .amount(request.amount())
                .description("Remboursement — " + debt.getPersonName())
                .transactionDate(request.paymentDate())
                .build();
        transactionRepository.save(transaction);

        DebtPayment payment = DebtPayment.builder()
                .debt(debt)
                .account(account)
                .amount(request.amount())
                .paymentDate(request.paymentDate())
                .build();
        payment = debtPaymentRepository.save(payment);

        BigDecimal totalPaid = debtPaymentRepository.sumAmountByDebtId(debt.getId());
        recomputeStatus(debt, totalPaid);
        debtRepository.save(debt);

        return DebtPaymentResponse.from(payment);
    }

    private void recomputeStatus(Debt debt, BigDecimal totalPaid) {
        if (totalPaid.compareTo(debt.getAmount()) >= 0) {
            debt.setStatus(DebtStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            debt.setStatus(DebtStatus.PARTIALLY_PAID);
        } else {
            debt.setStatus(DebtStatus.OPEN);
        }
    }

    private Debt getOwnedDebt(Long userId, Long debtId) {
        return debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dette introuvable"));
    }
}

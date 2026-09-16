package com.volako.backend.repository;

import com.volako.backend.domain.Transaction;
import com.volako.backend.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByAccountId(Long accountId);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.account.id = :accountId and t.type = :type")
    BigDecimal sumAmountByAccountIdAndType(@Param("accountId") Long accountId, @Param("type") TransactionType type);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.user.id = :userId and t.type = :type "
            + "and t.transactionDate >= :from and t.transactionDate <= :to")
    BigDecimal sumAmountByUserIdAndTypeAndPeriod(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}

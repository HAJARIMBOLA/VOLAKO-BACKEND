package com.volako.backend.repository;

import com.volako.backend.domain.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    List<DebtPayment> findByDebtIdOrderByPaymentDateDesc(Long debtId);

    @Query("select coalesce(sum(p.amount), 0) from DebtPayment p where p.debt.id = :debtId")
    BigDecimal sumAmountByDebtId(@Param("debtId") Long debtId);
}

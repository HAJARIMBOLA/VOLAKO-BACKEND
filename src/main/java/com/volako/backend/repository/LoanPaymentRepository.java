package com.volako.backend.repository;

import com.volako.backend.domain.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    List<LoanPayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    long countByLoanId(Long loanId);

    @Query("select coalesce(sum(p.amount), 0) from LoanPayment p where p.loan.id = :loanId")
    BigDecimal sumAmountByLoanId(@Param("loanId") Long loanId);
}

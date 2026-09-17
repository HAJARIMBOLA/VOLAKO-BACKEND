package com.volako.backend.repository;

import com.volako.backend.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdOrderByStartDateDescCreatedAtDesc(Long userId);

    Optional<Loan> findByIdAndUserId(Long id, Long userId);
}

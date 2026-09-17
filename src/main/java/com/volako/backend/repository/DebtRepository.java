package com.volako.backend.repository;

import com.volako.backend.domain.Debt;
import com.volako.backend.domain.enums.DebtDirection;
import com.volako.backend.domain.enums.DebtStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByUserIdOrderByDueDateAscCreatedAtDesc(Long userId);

    List<Debt> findByUserIdAndDirectionOrderByDueDateAscCreatedAtDesc(Long userId, DebtDirection direction);

    Optional<Debt> findByIdAndUserId(Long id, Long userId);

    @Query("select d from Debt d where d.user.id = :userId and d.status <> :paidStatus and d.dueDate is not null and d.dueDate < :today")
    List<Debt> findOverdue(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("paidStatus") DebtStatus paidStatus);
}

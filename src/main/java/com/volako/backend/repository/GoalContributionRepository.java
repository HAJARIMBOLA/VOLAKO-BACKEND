package com.volako.backend.repository;

import com.volako.backend.domain.GoalContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findByGoalIdOrderByContributionDateDesc(Long goalId);

    @Query("select coalesce(sum(c.amount), 0) from GoalContribution c where c.goal.id = :goalId")
    BigDecimal sumAmountByGoalId(@Param("goalId") Long goalId);
}

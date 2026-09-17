package com.volako.backend.dto.goal;

import com.volako.backend.domain.FinancialGoal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal savedAmount,
        BigDecimal remainingAmount,
        int progressPercent,
        LocalDate targetDate,
        boolean completed
) {
    public static GoalResponse from(FinancialGoal goal, BigDecimal savedAmount) {
        BigDecimal remaining = goal.getTargetAmount().subtract(savedAmount).max(BigDecimal.ZERO);
        int progress = goal.getTargetAmount().signum() == 0
                ? 0
                : Math.min(100, savedAmount.multiply(BigDecimal.valueOf(100))
                        .divide(goal.getTargetAmount(), 0, java.math.RoundingMode.DOWN)
                        .intValue());

        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                savedAmount,
                remaining,
                progress,
                goal.getTargetDate(),
                savedAmount.compareTo(goal.getTargetAmount()) >= 0
        );
    }
}

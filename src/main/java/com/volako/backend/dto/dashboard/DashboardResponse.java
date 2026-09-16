package com.volako.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardResponse(
        BigDecimal totalBalance,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        LocalDate periodFrom,
        LocalDate periodTo
) {
}

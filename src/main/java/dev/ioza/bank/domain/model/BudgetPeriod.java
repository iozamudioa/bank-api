package dev.ioza.bank.domain.model;

import dev.ioza.bank.domain.enums.PeriodStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetPeriod(
    String id,
    String householdId,
    LocalDate startDate,
    LocalDate endDate,
    PeriodStatus status,
    BigDecimal expectedIncome,
    List<BudgetLine> lines) {}

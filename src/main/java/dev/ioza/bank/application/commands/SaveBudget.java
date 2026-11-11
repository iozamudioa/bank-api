package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.model.BudgetLine;
import java.math.BigDecimal;
import java.util.List;

public record SaveBudget(BigDecimal expectedIncome, List<BudgetLine> allocations) {}

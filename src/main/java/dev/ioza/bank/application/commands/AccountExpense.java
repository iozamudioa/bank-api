package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record AccountExpense(
    String periodId, String accountId, BigDecimal amount, String description) {}

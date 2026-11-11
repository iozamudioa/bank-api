package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record Transfer(
    String periodId,
    String fromAccountId,
    String toAccountId,
    BigDecimal amount,
    String description) {}

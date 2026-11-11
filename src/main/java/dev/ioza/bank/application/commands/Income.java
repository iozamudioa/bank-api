package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record Income(
    String periodId,
    String accountId,
    String incomeSourceId,
    BigDecimal amount,
    String description) {}

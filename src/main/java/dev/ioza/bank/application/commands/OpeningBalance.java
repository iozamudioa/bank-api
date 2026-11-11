package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record OpeningBalance(
    String periodId, String accountId, BigDecimal amount, String description) {}

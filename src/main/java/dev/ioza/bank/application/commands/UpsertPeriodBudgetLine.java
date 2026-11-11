package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record UpsertPeriodBudgetLine(String envelopeId, BigDecimal amount) {}

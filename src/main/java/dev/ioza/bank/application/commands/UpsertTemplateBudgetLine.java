package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record UpsertTemplateBudgetLine(String envelopeId, BigDecimal amount) {}

package dev.ioza.bank.domain.model;

import java.math.BigDecimal;

public record BudgetLine(String envelopeId, BigDecimal amount) {}

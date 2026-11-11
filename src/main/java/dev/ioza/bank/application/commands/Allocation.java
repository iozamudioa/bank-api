package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record Allocation(
    String periodId, String envelopeId, BigDecimal amount, String description) {}

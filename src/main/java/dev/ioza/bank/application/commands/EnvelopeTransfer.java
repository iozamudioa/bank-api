package dev.ioza.bank.application.commands;

import java.math.BigDecimal;

public record EnvelopeTransfer(
    String periodId,
    String fromEnvelopeId,
    String toEnvelopeId,
    BigDecimal amount,
    String description) {}

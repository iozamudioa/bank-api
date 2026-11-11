package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.model.Funding;
import java.math.BigDecimal;
import java.util.List;

public record Expense(
    String periodId,
    String targetEnvelopeId,
    BigDecimal amount,
    String description,
    List<Funding> fundingSources) {}

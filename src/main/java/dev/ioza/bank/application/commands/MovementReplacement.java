package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.model.Funding;
import java.math.BigDecimal;
import java.util.List;

/** Complete replacement values; period comes from the correction command. */
public record MovementReplacement(
    MovementType type,
    BigDecimal amount,
    String description,
    String accountId,
    String incomeSourceId,
    String envelopeId,
    String fromAccountId,
    String toAccountId,
    String fromEnvelopeId,
    String toEnvelopeId,
    List<Funding> fundingSources) {}

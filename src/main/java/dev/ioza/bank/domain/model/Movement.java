package dev.ioza.bank.domain.model;

import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Movement(
    String id,
    String householdId,
    String periodId,
    MovementType type,
    MovementStatus status,
    BigDecimal amount,
    String description,
    String targetEnvelopeId,
    String incomeSourceId,
    String actor,
    Instant occurredAt,
    String reversalOf,
    List<Funding> funding,
    List<BalanceEffect> accountEffects,
    List<BalanceEffect> envelopeEffects,
    CorrectionLink correction) {
  public Movement(
      String id,
      String householdId,
      String periodId,
      MovementType type,
      MovementStatus status,
      BigDecimal amount,
      String description,
      String targetEnvelopeId,
      String incomeSourceId,
      String actor,
      Instant occurredAt,
      String reversalOf,
      List<Funding> funding,
      List<BalanceEffect> accountEffects,
      List<BalanceEffect> envelopeEffects) {
    this(
        id,
        householdId,
        periodId,
        type,
        status,
        amount,
        description,
        targetEnvelopeId,
        incomeSourceId,
        actor,
        occurredAt,
        reversalOf,
        funding,
        accountEffects,
        envelopeEffects,
        null);
  }
}

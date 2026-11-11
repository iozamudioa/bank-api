package dev.ioza.bank.domain.model;

import dev.ioza.bank.domain.enums.EnvelopeType;
import java.math.BigDecimal;

public record Envelope(
    String id,
    String householdId,
    String name,
    String accountId,
    EnvelopeType type,
    String category,
    BigDecimal balance,
    Long version,
    boolean active) {
  public Envelope withBalance(BigDecimal next) {
    return new Envelope(id, householdId, name, accountId, type, category, next, version, active);
  }
}

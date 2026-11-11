package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.enums.EnvelopeType;
import java.math.BigDecimal;

public record CreateEnvelope(
    String name, String accountId, EnvelopeType type, String category, BigDecimal habitualAmount) {
  public CreateEnvelope(String name, String accountId, EnvelopeType type, String category) {
    this(name, accountId, type, category, null);
  }
}

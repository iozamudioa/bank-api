package dev.ioza.bank.domain.model;

import dev.ioza.bank.domain.enums.AccountType;
import java.math.BigDecimal;

public record Account(
    String id,
    String householdId,
    String name,
    AccountType type,
    BigDecimal balance,
    Long version,
    boolean active) {
  public Account withBalance(BigDecimal next) {
    return new Account(id, householdId, name, type, next, version, active);
  }
}

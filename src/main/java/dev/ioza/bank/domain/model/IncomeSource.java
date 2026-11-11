package dev.ioza.bank.domain.model;

import dev.ioza.bank.domain.enums.IncomeSourceType;
import java.math.BigDecimal;

public record IncomeSource(
    String id,
    String householdId,
    String name,
    IncomeSourceType type,
    String defaultAccountId,
    BigDecimal expectedAmount,
    boolean active) {}

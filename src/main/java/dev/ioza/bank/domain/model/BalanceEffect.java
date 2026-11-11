package dev.ioza.bank.domain.model;

import java.math.BigDecimal;

public record BalanceEffect(
    String entityId, BigDecimal before, BigDecimal change, BigDecimal after) {}

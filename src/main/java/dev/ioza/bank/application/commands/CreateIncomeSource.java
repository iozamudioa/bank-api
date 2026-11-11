package dev.ioza.bank.application.commands;

import dev.ioza.bank.domain.enums.IncomeSourceType;
import java.math.BigDecimal;

public record CreateIncomeSource(
    String name, IncomeSourceType type, String accountId, BigDecimal expectedAmount) {}

package dev.ioza.bank.application.ports.in;

import java.math.BigDecimal;

public record AccountExpensePreview(
    String accountId,
    BigDecimal amount,
    BigDecimal totalBefore,
    BigDecimal freeBefore,
    BigDecimal totalAfter,
    BigDecimal freeAfter) {}

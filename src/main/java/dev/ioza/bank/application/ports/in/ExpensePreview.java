package dev.ioza.bank.application.ports.in;

import java.math.BigDecimal;
import java.util.List;

public record ExpensePreview(
    BigDecimal requestedAmount,
    BigDecimal targetBalance,
    BigDecimal shortfall,
    List<DonorOption> eligibleDonors) {}

package dev.ioza.bank.application.ports.in;

import java.math.BigDecimal;
import java.util.List;

public record DashboardView(
    BigDecimal total, BigDecimal reserved, BigDecimal free, List<AccountBalance> accounts) {}

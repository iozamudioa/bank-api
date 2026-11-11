package dev.ioza.bank.application.ports.in;

import java.math.BigDecimal;

public record AccountBalance(
    String id, String name, String type, BigDecimal total, BigDecimal reserved, BigDecimal free) {}

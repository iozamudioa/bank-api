package dev.ioza.bank.domain.model;

import java.math.BigDecimal;

public record Funding(String envelopeId, BigDecimal amount) {}

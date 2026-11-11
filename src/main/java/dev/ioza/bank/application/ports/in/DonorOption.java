package dev.ioza.bank.application.ports.in;

import java.math.BigDecimal;

public record DonorOption(String envelopeId, String name, BigDecimal balance) {}

package dev.ioza.bank.domain.model;

import java.math.BigDecimal;

/** Historical response snapshot for creation retries; not the current template value. */
public record EnvelopeCreationRecord(
    String id,
    String householdId,
    String fingerprint,
    Envelope envelope,
    BigDecimal habitualAmount) {}

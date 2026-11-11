package dev.ioza.bank.domain.model;

/** Immutable resource identifiers captured from the confirmed replacement command. */
public record ReplacementResources(
    String accountId,
    String envelopeId,
    String fromAccountId,
    String toAccountId,
    String fromEnvelopeId,
    String toEnvelopeId,
    String incomeSourceId) {}

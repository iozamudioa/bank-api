package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

public record ReplacementResourcesDoc(
    String accountId,
    String envelopeId,
    String fromAccountId,
    String toAccountId,
    String fromEnvelopeId,
    String toEnvelopeId,
    String incomeSourceId) {}

package dev.ioza.bank.domain.model;

public record IdempotencyRecord(
    String id, String householdId, String fingerprint, Movement response) {}

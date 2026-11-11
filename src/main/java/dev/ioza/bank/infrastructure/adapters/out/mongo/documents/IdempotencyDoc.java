package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("idempotency_records")
public record IdempotencyDoc(
    @Id String id, String householdId, String fingerprint, MovementDoc response) {}

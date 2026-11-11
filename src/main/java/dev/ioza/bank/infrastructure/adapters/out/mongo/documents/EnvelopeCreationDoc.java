package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("envelope_creation_requests")
public record EnvelopeCreationDoc(
    @Id String id,
    String householdId,
    String fingerprint,
    EnvelopeDoc envelope,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal habitualAmount) {}

package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import java.math.BigDecimal;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public record BudgetLineDoc(
    String envelopeId, @Field(targetType = FieldType.DECIMAL128) BigDecimal amount) {}

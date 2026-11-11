package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import dev.ioza.bank.domain.enums.IncomeSourceType;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("income_sources")
public record IncomeSourceDoc(
    @Id String id,
    String householdId,
    String name,
    IncomeSourceType type,
    String defaultAccountId,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal expectedAmount,
    boolean active) {}

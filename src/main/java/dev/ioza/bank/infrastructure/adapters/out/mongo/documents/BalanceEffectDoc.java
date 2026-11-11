package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import java.math.BigDecimal;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public record BalanceEffectDoc(
    String entityId,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal before,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal change,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal after) {}

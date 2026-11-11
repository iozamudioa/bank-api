package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("budget_templates")
public record BudgetTemplateDoc(
    @Id String id,
    @Indexed(unique = true) String householdId,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal expectedIncome,
    List<BudgetLineDoc> lines) {}

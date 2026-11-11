package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import dev.ioza.bank.domain.enums.PeriodStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("budget_periods")
@CompoundIndex(
    name = "period_household_start",
    def = "{'householdId':1,'startDate':1}",
    unique = true)
public record BudgetPeriodDoc(
    @Id String id,
    @Indexed String householdId,
    LocalDate startDate,
    LocalDate endDate,
    PeriodStatus status,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal expectedIncome,
    List<BudgetLineDoc> lines) {}

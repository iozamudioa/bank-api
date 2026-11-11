package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import dev.ioza.bank.domain.enums.AccountType;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("accounts")
public record AccountDoc(
    @Id String id,
    @Indexed String householdId,
    String name,
    AccountType type,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal balance,
    @Version Long version,
    boolean active) {}

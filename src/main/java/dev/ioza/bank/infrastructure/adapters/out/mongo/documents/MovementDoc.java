package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("movements")
@CompoundIndexes({
  @CompoundIndex(
      name = "movement_household_time",
      def = "{'householdId':1,'occurredAt':-1,'_id':-1}"),
  @CompoundIndex(
      name = "unique_reversal",
      def = "{'reversalOf':1}",
      unique = true,
      partialFilter = "{'reversalOf':{'$type':'string'}}")
})
public record MovementDoc(
    @Id String id,
    @Indexed String householdId,
    String periodId,
    MovementType type,
    MovementStatus status,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal amount,
    String description,
    String targetEnvelopeId,
    String incomeSourceId,
    String actor,
    Instant occurredAt,
    String reversalOf,
    List<FundingDoc> funding,
    List<BalanceEffectDoc> accountEffects,
    List<BalanceEffectDoc> envelopeEffects,
    CorrectionLinkDoc correction) {}

package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.ioza.bank.application.commands.MovementReplacement;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = IncomeReplacementRequest.class, name = "INCOME"),
  @JsonSubTypes.Type(value = AllocationReplacementRequest.class, name = "ALLOCATE"),
  @JsonSubTypes.Type(value = ReleaseReplacementRequest.class, name = "RELEASE"),
  @JsonSubTypes.Type(value = ReallocationReplacementRequest.class, name = "REALLOCATE"),
  @JsonSubTypes.Type(value = TransferReplacementRequest.class, name = "TRANSFER"),
  @JsonSubTypes.Type(value = EnvelopeTransferReplacementRequest.class, name = "ENVELOPE_TRANSFER"),
  @JsonSubTypes.Type(value = ExpenseReplacementRequest.class, name = "EXPENSE"),
  @JsonSubTypes.Type(value = AccountExpenseReplacementRequest.class, name = "ACCOUNT_EXPENSE")
})
@Schema(
    description =
        "Reemplazo completo. EXPENSE usa sobres; ACCOUNT_EXPENSE usa saldo libre. Ambos conservan"
            + " MovementType.EXPENSE.",
    discriminatorProperty = "type",
    oneOf = {
      IncomeReplacementRequest.class,
      AllocationReplacementRequest.class,
      ReleaseReplacementRequest.class,
      ReallocationReplacementRequest.class,
      TransferReplacementRequest.class,
      EnvelopeTransferReplacementRequest.class,
      ExpenseReplacementRequest.class,
      AccountExpenseReplacementRequest.class
    })
public interface MovementReplacementRequest {
  MovementReplacement toCommand();
}

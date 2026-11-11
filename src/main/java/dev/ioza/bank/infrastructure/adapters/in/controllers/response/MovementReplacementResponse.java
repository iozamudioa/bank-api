package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.model.Movement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description =
        "Comando de reemplazo confirmado con recursos explicitos historicos. Permite editar otra"
            + " vez incluso con importe cero; no se deduce de efectos o saldos actuales.")
public record MovementReplacementResponse(
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {
              "INCOME",
              "ALLOCATE",
              "RELEASE",
              "REALLOCATE",
              "TRANSFER",
              "ENVELOPE_TRANSFER",
              "EXPENSE",
              "ACCOUNT_EXPENSE"
            })
        String type,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            minimum = "0")
        BigDecimal amount,
    @Schema(nullable = true) String description,
    @Schema(nullable = true) String accountId,
    @Schema(nullable = true) String incomeSourceId,
    @Schema(nullable = true) String envelopeId,
    @Schema(nullable = true) String fromAccountId,
    @Schema(nullable = true) String toAccountId,
    @Schema(nullable = true) String fromEnvelopeId,
    @Schema(nullable = true) String toEnvelopeId,
    @Schema(nullable = true) String targetEnvelopeId,
    List<FundingResponse> fundingSources) {
  public static MovementReplacementResponse from(Movement m) {
    if (m.correction() == null || m.correction().replacement() == null) return null;
    var r = m.correction().replacement();
    String type =
        m.type() == MovementType.EXPENSE && r.accountId() != null
            ? "ACCOUNT_EXPENSE"
            : m.type().name();
    return new MovementReplacementResponse(
        type,
        m.amount(),
        m.description(),
        r.accountId(),
        r.incomeSourceId(),
        m.type() == MovementType.EXPENSE ? null : r.envelopeId(),
        r.fromAccountId(),
        r.toAccountId(),
        r.fromEnvelopeId(),
        r.toEnvelopeId(),
        m.type() == MovementType.EXPENSE ? r.envelopeId() : null,
        m.type() == MovementType.EXPENSE && r.accountId() == null
            ? m.funding().stream().map(FundingResponse::from).toList()
            : List.of());
  }
}

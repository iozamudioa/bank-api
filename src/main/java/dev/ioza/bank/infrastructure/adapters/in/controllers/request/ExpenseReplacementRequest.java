package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.MovementReplacement;
import dev.ioza.bank.domain.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description = "Reemplazo EXPENSE; campos completos del nuevo movimiento.",
    example =
        "{\"type\":\"EXPENSE\",\"amount\":150,\"description\":\"Corregido\",\"targetEnvelopeId\":\"envelope-central\"}")
public record ExpenseReplacementRequest(
    @NotBlank String targetEnvelopeId,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<@NotNull @Valid FundingRequest> fundingSources,
    @Schema(
            type = "number",
            format = "decimal",
            example = "150",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount,
    String description)
    implements MovementReplacementRequest {
  public MovementReplacement toCommand() {
    return new MovementReplacement(
        MovementType.EXPENSE,
        amount,
        description,
        null,
        null,
        targetEnvelopeId,
        null,
        null,
        null,
        null,
        fundingSources == null
            ? null
            : fundingSources.stream().map(FundingRequest::toDomain).toList());
  }
}

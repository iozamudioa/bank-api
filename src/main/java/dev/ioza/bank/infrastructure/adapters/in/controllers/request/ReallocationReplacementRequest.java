package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.MovementReplacement;
import dev.ioza.bank.domain.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description = "Reemplazo REALLOCATE; campos completos del nuevo movimiento.",
    example =
        "{\"type\":\"REALLOCATE\",\"amount\":150,\"description\":\"Corregido\",\"fromEnvelopeId\":\"envelope-central\",\"toEnvelopeId\":\"envelope-central\"}")
public record ReallocationReplacementRequest(
    @NotBlank String fromEnvelopeId,
    @NotBlank String toEnvelopeId,
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
        MovementType.REALLOCATE,
        amount,
        description,
        null,
        null,
        null,
        null,
        null,
        fromEnvelopeId,
        toEnvelopeId,
        null);
  }
}

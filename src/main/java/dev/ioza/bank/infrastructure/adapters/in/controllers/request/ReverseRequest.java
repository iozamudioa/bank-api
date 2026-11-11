package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.Reverse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description =
        "ReverseRequest: entrada REST; validaciones Jakarta y reglas adicionales en la operación.",
    example = "{\"periodId\":\"period-q1\",\"reason\":\"Captura equivocada\"}")
public record ReverseRequest(
    @Schema(
            description =
                "Quincena del movimiento; debe estar OPEN al confirmar. En saldo inicial puede"
                    + " omitirse y se ignora.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "period-q1")
        @NotBlank
        String periodId,
    @Schema(
            description = "Motivo obligatorio de la reversión.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Captura equivocada")
        @NotBlank
        String reason) {
  public Reverse toCommand() {
    return new Reverse(periodId, reason);
  }
}

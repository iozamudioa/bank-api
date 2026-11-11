package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.Reallocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "ReallocationRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example =
        "{\"periodId\":\"period-q1\",\"fromEnvelopeId\":\"envelope-sams\",\"toEnvelopeId\":\"envelope-central\",\"amount\":70,\"description\":\"Apoyo"
            + " de presupuesto\"}")
public record ReallocationRequest(
    @Schema(
            description =
                "Quincena del movimiento; debe estar OPEN al confirmar. En saldo inicial puede"
                    + " omitirse y se ignora.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "period-q1")
        @NotBlank
        String periodId,
    @Schema(
            description =
                "Identificador fromEnvelopeId del mismo hogar; origen y destino deben ser"
                    + " distintos.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "envelope-sams")
        @NotBlank
        String fromEnvelopeId,
    @Schema(
            description =
                "Identificador toEnvelopeId del mismo hogar; origen y destino deben ser distintos.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "envelope-central")
        @NotBlank
        String toEnvelopeId,
    @Schema(
            description =
                "Importe decimal MXN. Número JSON, máximo dos decimales y 32 dígitos enteros; sin"
                    + " redondeo implícito. Se serializa como número JSON decimal, nunca Extended"
                    + " JSON de MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "150")
        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount,
    @Schema(
            description = "Concepto opcional del movimiento.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            example = "Carne")
        String description) {
  public Reallocation toCommand() {
    return new Reallocation(periodId, fromEnvelopeId, toEnvelopeId, amount, description);
  }
}

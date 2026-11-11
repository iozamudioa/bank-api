package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.UpsertPeriodBudgetLine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description = "Importe planeado para un sobre en una quincena abierta.",
    example = "{\"amount\":1500}")
public record UpsertPeriodBudgetLineRequest(
    @Schema(
            description = "Importe MXN no negativo; maximo dos decimales, sin redondeo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "1500")
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount) {
  public UpsertPeriodBudgetLine toCommand(String envelopeId) {
    return new UpsertPeriodBudgetLine(envelopeId, amount);
  }
}

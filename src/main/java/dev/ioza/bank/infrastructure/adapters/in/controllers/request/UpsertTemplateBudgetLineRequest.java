package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.UpsertTemplateBudgetLine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description = "Plan habitual de un solo sobre; no requiere quincena.",
    example = "{\"amount\":4800}")
public record UpsertTemplateBudgetLineRequest(
    @Schema(
            description = "Importe no negativo, maximo dos decimales sin redondeo. Cero es valido.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "4800")
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount) {
  public UpsertTemplateBudgetLine toCommand(String envelopeId) {
    return new UpsertTemplateBudgetLine(envelopeId, amount);
  }
}

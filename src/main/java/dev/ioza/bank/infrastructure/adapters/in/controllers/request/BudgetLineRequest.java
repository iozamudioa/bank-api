package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.domain.model.BudgetLine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "BudgetLineRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example = "{\"envelopeId\":\"envelope-central\",\"amount\":4500}")
public record BudgetLineRequest(
    @Schema(
            description = "Identificador del sobre del hogar.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "envelope-central")
        @NotBlank
        String envelopeId,
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
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount) {
  public BudgetLine toDomain() {
    return new BudgetLine(envelopeId, amount);
  }
}

package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.model.BudgetLine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "BudgetLineResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record BudgetLineResponse(
    @Schema(
            description = "Identificador del sobre del hogar.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "envelope-central",
            accessMode = Schema.AccessMode.READ_ONLY)
        String envelopeId,
    @Schema(
            description =
                "Importe decimal MXN. Número JSON, máximo dos decimales y 32 dígitos enteros; sin"
                    + " redondeo implícito. Se serializa como número JSON decimal, nunca Extended"
                    + " JSON de MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "150",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal amount) {
  public static BudgetLineResponse from(BudgetLine value) {
    return new BudgetLineResponse(value.envelopeId(), value.amount());
  }
}

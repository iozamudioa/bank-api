package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.enums.IncomeSourceType;
import dev.ioza.bank.domain.model.IncomeSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "IncomeSourceResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record IncomeSourceResponse(
    @Schema(
            description = "Identificador opaco; no interpretar ni generar en el frontend.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash",
            accessMode = Schema.AccessMode.READ_ONLY)
        String id,
    @Schema(
            description = "Identificador del hogar al que pertenece el recurso.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "household-demo",
            accessMode = Schema.AccessMode.READ_ONLY)
        String householdId,
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar",
            accessMode = Schema.AccessMode.READ_ONLY)
        String name,
    @Schema(
            description = "Tipo del recurso según el enum del contrato.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "PAYROLL",
            accessMode = Schema.AccessMode.READ_ONLY)
        IncomeSourceType type,
    @Schema(
            description =
                "Cuenta sugerida de la fuente, no se usa automáticamente en el request de ingreso.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-digital",
            accessMode = Schema.AccessMode.READ_ONLY)
        String defaultAccountId,
    @Schema(
            description =
                "Importe esperado opcional y no negativo; no representa un ingreso real. Se"
                    + " serializa como número JSON decimal, nunca Extended JSON de MongoDB; el"
                    + " frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            type = "number",
            format = "decimal",
            example = "150",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal expectedAmount,
    @Schema(
            description = "Indica si el recurso o membresía está activo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY)
        boolean active) {
  public static IncomeSourceResponse from(IncomeSource value) {
    return new IncomeSourceResponse(
        value.id(),
        value.householdId(),
        value.name(),
        value.type(),
        value.defaultAccountId(),
        value.expectedAmount(),
        value.active());
  }
}

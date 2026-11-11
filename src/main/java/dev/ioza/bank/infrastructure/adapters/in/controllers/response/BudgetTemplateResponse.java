package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.model.BudgetTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description =
        "BudgetTemplateResponse: respuesta REST independiente de MongoDB; importes numéricos y"
            + " campos auditados.")
public record BudgetTemplateResponse(
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
            description =
                "Ingreso planificado no negativo; no representa depósito ni reserva. Se serializa"
                    + " como número JSON decimal, nunca Extended JSON de MongoDB; el frontend debe"
                    + " usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "150",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal expectedIncome,
    @Schema(
            description = "Snapshot de líneas planificadas; no equivale a saldo reservado.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
        List<BudgetLineResponse> lines) {
  public static BudgetTemplateResponse from(BudgetTemplate value) {
    return new BudgetTemplateResponse(
        value.id(),
        value.householdId(),
        value.expectedIncome(),
        value.lines().stream().map(BudgetLineResponse::from).toList());
  }
}

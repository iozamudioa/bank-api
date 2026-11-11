package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.model.BudgetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(
    description =
        "BudgetPeriodResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record BudgetPeriodResponse(
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
            description = "Fecha local YYYY-MM-DD, sin hora ni offset.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-11-15",
            accessMode = Schema.AccessMode.READ_ONLY)
        LocalDate startDate,
    @Schema(
            description =
                "Fecha local YYYY-MM-DD; no anterior a startDate ni traslapada con otro periodo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-11-30",
            accessMode = Schema.AccessMode.READ_ONLY)
        LocalDate endDate,
    @Schema(
            description = "Estado persistido del recurso.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "OPEN",
            accessMode = Schema.AccessMode.READ_ONLY)
        PeriodStatus status,
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
  public static BudgetPeriodResponse from(BudgetPeriod value) {
    return new BudgetPeriodResponse(
        value.id(),
        value.householdId(),
        value.startDate(),
        value.endDate(),
        value.status(),
        value.expectedIncome(),
        value.lines().stream().map(BudgetLineResponse::from).toList());
  }
}

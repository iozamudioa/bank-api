package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.CreatePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(
    description =
        "CreatePeriodRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example = "{\"startDate\":\"2026-11-15\",\"endDate\":\"2026-11-30\",\"open\":true}")
public record CreatePeriodRequest(
    @Schema(
            description = "Fecha local YYYY-MM-DD, sin hora ni offset.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-11-15")
        @NotNull
        LocalDate startDate,
    @Schema(
            description =
                "Fecha local YYYY-MM-DD; no anterior a startDate ni traslapada con otro periodo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-11-30")
        @NotNull
        LocalDate endDate,
    @Schema(
            description =
                "true crea OPEN; omitido o false crea DRAFT. No existe apertura posterior de"
                    + " DRAFT.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "true")
        boolean open) {
  public CreatePeriod toCommand() {
    return new CreatePeriod(startDate, endDate, open);
  }
}

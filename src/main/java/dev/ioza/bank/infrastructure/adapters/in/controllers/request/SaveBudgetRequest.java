package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.SaveBudget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description =
        "SaveBudgetRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example =
        "{\"expectedIncome\":21000,\"allocations\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}")
public record SaveBudgetRequest(
    @Schema(
            description =
                "Ingreso planificado no negativo; no representa depósito ni reserva. Se serializa"
                    + " como número JSON decimal, nunca Extended JSON de MongoDB; el frontend debe"
                    + " usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "150")
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal expectedIncome,
    @Schema(
            description =
                "Líneas del plan: sobres activos del hogar, sin duplicados, importes no negativos."
                    + " Puede ser lista vacía.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        List<@NotNull @Valid BudgetLineRequest> allocations) {
  public SaveBudget toCommand() {
    return new SaveBudget(
        expectedIncome,
        allocations == null ? null : allocations.stream().map(x -> x.toDomain()).toList());
  }
}

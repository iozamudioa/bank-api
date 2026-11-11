package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.CreateIncomeSource;
import dev.ioza.bank.domain.enums.IncomeSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "CreateIncomeSourceRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example =
        "{\"name\":\"Nómina\",\"type\":\"PAYROLL\",\"accountId\":\"account-digital\",\"expectedAmount\":21000}")
public record CreateIncomeSourceRequest(
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar")
        @NotBlank
        String name,
    @Schema(
            description = "Tipo del recurso según el enum del contrato.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "PAYROLL")
        @NotNull
        IncomeSourceType type,
    @Schema(
            description = "Cuenta activa del mismo hogar. Determina dónde está el dinero real.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash")
        @NotBlank
        String accountId,
    @Schema(
            description =
                "Importe esperado opcional y no negativo; no representa un ingreso real. Se"
                    + " serializa como número JSON decimal, nunca Extended JSON de MongoDB; el"
                    + " frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            type = "number",
            format = "decimal",
            example = "150")
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal expectedAmount) {
  public CreateIncomeSource toCommand() {
    return new CreateIncomeSource(name, type, accountId, expectedAmount);
  }
}

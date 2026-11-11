package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.Income;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "IncomeRequest: entrada REST; validaciones Jakarta y reglas adicionales en la operación.",
    example =
        "{\"periodId\":\"period-q1\",\"accountId\":\"account-digital\",\"incomeSourceId\":\"income-payroll\",\"amount\":21000,\"description\":\"Nómina\"}")
public record IncomeRequest(
    @Schema(
            description =
                "Quincena del movimiento; debe estar OPEN al confirmar. En saldo inicial puede"
                    + " omitirse y se ignora.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "period-q1")
        @NotBlank
        String periodId,
    @Schema(
            description = "Cuenta activa del mismo hogar. Determina dónde está el dinero real.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash")
        @NotBlank
        String accountId,
    @Schema(
            description =
                "Fuente activa del hogar, opcional. No determina automáticamente la cuenta del"
                    + " ingreso.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            example = "income-payroll")
        String incomeSourceId,
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
        @DecimalMin("0.01")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount,
    @Schema(
            description = "Concepto opcional del movimiento.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            example = "Carne")
        String description) {
  public Income toCommand() {
    return new Income(periodId, accountId, incomeSourceId, amount, description);
  }
}

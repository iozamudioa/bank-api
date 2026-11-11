package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.OpeningBalance;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "OpeningBalanceRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example =
        "{\"accountId\":\"account-cash\",\"amount\":1080,\"description\":\"Saldo inicial real\"}")
public record OpeningBalanceRequest(
    @Schema(
            description =
                "Quincena del movimiento; debe estar OPEN al confirmar. En saldo inicial puede"
                    + " omitirse y se ignora.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            example = "period-q1")
        String periodId,
    @Schema(
            description = "Cuenta activa del mismo hogar. Determina dónde está el dinero real.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash")
        @NotBlank
        String accountId,
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
  public OpeningBalance toCommand() {
    return new OpeningBalance(periodId, accountId, amount, description);
  }
}

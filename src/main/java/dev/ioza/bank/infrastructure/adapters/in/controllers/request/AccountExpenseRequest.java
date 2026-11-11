package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.AccountExpense;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description = "Gasto desde saldo libre; no consume reservas.",
    example =
        "{\"periodId\":\"period-q1\",\"accountId\":\"account-cash\",\"amount\":150,\"description\":\"Gasto"
            + " directo\"}")
public record AccountExpenseRequest(
    @NotBlank String periodId,
    @NotBlank String accountId,
    @Schema(
            type = "number",
            format = "decimal",
            example = "150",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 32, fraction = 2)
        BigDecimal amount,
    String description) {
  public AccountExpense toCommand() {
    return new AccountExpense(periodId, accountId, amount, description);
  }
}

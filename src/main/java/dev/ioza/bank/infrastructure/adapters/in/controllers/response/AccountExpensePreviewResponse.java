package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.AccountExpensePreview;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Vista previa sin escrituras; confirmar revalida disponibilidad.")
public record AccountExpensePreviewResponse(
    String accountId,
    @Schema(type = "number", format = "decimal") BigDecimal amount,
    @Schema(type = "number", format = "decimal") BigDecimal totalBefore,
    @Schema(type = "number", format = "decimal") BigDecimal freeBefore,
    @Schema(type = "number", format = "decimal") BigDecimal totalAfter,
    @Schema(type = "number", format = "decimal") BigDecimal freeAfter) {
  public static AccountExpensePreviewResponse from(AccountExpensePreview p) {
    return new AccountExpensePreviewResponse(
        p.accountId(), p.amount(), p.totalBefore(), p.freeBefore(), p.totalAfter(), p.freeAfter());
  }
}

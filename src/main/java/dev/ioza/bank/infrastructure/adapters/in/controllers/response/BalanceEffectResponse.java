package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.model.BalanceEffect;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "BalanceEffectResponse: respuesta REST independiente de MongoDB; importes numéricos y"
            + " campos auditados.")
public record BalanceEffectResponse(
    @Schema(
            description = "Identificador de la cuenta o sobre afectado.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash",
            accessMode = Schema.AccessMode.READ_ONLY)
        String entityId,
    @Schema(
            description =
                "Saldo antes de la operación. Se serializa como número JSON decimal, nunca Extended"
                    + " JSON de MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "1080",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal before,
    @Schema(
            description =
                "Cambio con signo; cero es posible para cuentas bloqueadas por una operación de"
                    + " reserva. Se serializa como número JSON decimal, nunca Extended JSON de"
                    + " MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "-150",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal change,
    @Schema(
            description =
                "Saldo después de la operación. Se serializa como número JSON decimal, nunca"
                    + " Extended JSON de MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "930",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal after) {
  public static BalanceEffectResponse from(BalanceEffect value) {
    return new BalanceEffectResponse(
        value.entityId(), value.before(), value.change(), value.after());
  }
}

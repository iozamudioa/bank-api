package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.enums.AccountType;
import dev.ioza.bank.domain.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "AccountResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record AccountResponse(
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
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar",
            accessMode = Schema.AccessMode.READ_ONLY)
        String name,
    @Schema(
            description = "Tipo del recurso según el enum del contrato.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "PHYSICAL",
            accessMode = Schema.AccessMode.READ_ONLY)
        AccountType type,
    @Schema(
            description =
                "Saldo persistido. En cuenta es dinero real; en sobre es dinero reservado dentro de"
                    + " su cuenta. Se serializa como número JSON decimal, nunca Extended JSON de"
                    + " MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "80",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal balance,
    @Schema(
            description =
                "Versión técnica optimista; sólo lectura, no se envía al registrar movimientos.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "0",
            accessMode = Schema.AccessMode.READ_ONLY)
        Long version,
    @Schema(
            description = "Indica si el recurso o membresía está activo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY)
        boolean active) {
  public static AccountResponse from(Account value) {
    return new AccountResponse(
        value.id(),
        value.householdId(),
        value.name(),
        value.type(),
        value.balance(),
        value.version(),
        value.active());
  }
}

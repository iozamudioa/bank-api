package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.AccountBalance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "AccountBalanceResponse: respuesta REST independiente de MongoDB; importes numéricos y"
            + " campos auditados.")
public record AccountBalanceResponse(
    @Schema(
            description = "Identificador opaco; no interpretar ni generar en el frontend.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash",
            accessMode = Schema.AccessMode.READ_ONLY)
        String id,
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
        String type,
    @Schema(
            description =
                "Dinero real acumulado; no sumar reserved a este valor. Se serializa como número"
                    + " JSON decimal, nunca Extended JSON de MongoDB; el frontend debe usar"
                    + " aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "1080",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal total,
    @Schema(
            description =
                "Parte de total reservada por sobres. Se serializa como número JSON decimal, nunca"
                    + " Extended JSON de MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "1080",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal reserved,
    @Schema(
            description =
                "Dinero realmente libre: total menos reserved. Se serializa como número JSON"
                    + " decimal, nunca Extended JSON de MongoDB; el frontend debe usar aritmética"
                    + " decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "0",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal free) {
  public static AccountBalanceResponse from(AccountBalance value) {
    return new AccountBalanceResponse(
        value.id(), value.name(), value.type(), value.total(), value.reserved(), value.free());
  }
}

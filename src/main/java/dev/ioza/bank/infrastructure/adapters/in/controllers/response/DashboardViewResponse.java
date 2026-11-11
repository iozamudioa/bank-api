package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.DashboardView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description =
        "DashboardViewResponse: respuesta REST independiente de MongoDB; importes numéricos y"
            + " campos auditados.")
public record DashboardViewResponse(
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
        BigDecimal free,
    @Schema(
            description = "Desglose de saldos por cuenta.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
        List<AccountBalanceResponse> accounts) {
  public static DashboardViewResponse from(DashboardView value) {
    return new DashboardViewResponse(
        value.total(),
        value.reserved(),
        value.free(),
        value.accounts().stream().map(AccountBalanceResponse::from).toList());
  }
}

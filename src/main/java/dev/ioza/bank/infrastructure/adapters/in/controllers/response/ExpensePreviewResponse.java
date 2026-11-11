package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.ExpensePreview;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description =
        "ExpensePreviewResponse: respuesta REST independiente de MongoDB; importes numéricos y"
            + " campos auditados.")
public record ExpensePreviewResponse(
    @Schema(
            description =
                "Importe solicitado. Se serializa como número JSON decimal, nunca Extended JSON de"
                    + " MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "150",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal requestedAmount,
    @Schema(
            description =
                "Saldo actual del sobre principal en la lectura de preview. Se serializa como"
                    + " número JSON decimal, nunca Extended JSON de MongoDB; el frontend debe usar"
                    + " aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "80",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal targetBalance,
    @Schema(
            description =
                "max(requestedAmount-targetBalance,0). No es una reserva. Se serializa como número"
                    + " JSON decimal, nunca Extended JSON de MongoDB; el frontend debe usar"
                    + " aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "70",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal shortfall,
    @Schema(
            description =
                "Sobres activos con saldo positivo de la misma cuenta cuando hay faltante. No"
                    + " garantiza que cada uno cubra el faltante completo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
        List<DonorOptionResponse> eligibleDonors) {
  public static ExpensePreviewResponse from(ExpensePreview value) {
    return new ExpensePreviewResponse(
        value.requestedAmount(),
        value.targetBalance(),
        value.shortfall(),
        value.eligibleDonors().stream().map(DonorOptionResponse::from).toList());
  }
}

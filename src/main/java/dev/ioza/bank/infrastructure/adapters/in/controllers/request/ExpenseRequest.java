package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.Expense;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(
    description =
        "ExpenseRequest: entrada REST; validaciones Jakarta y reglas adicionales en la operación.",
    example =
        "{\"periodId\":\"period-q1\",\"targetEnvelopeId\":\"envelope-central\",\"amount\":150,\"description\":\"Carne\",\"fundingSources\":[{\"envelopeId\":\"envelope-central\",\"amount\":80},{\"envelopeId\":\"envelope-sams\",\"amount\":70}]}")
public record ExpenseRequest(
    @Schema(
            description =
                "Quincena del movimiento; debe estar OPEN al confirmar. En saldo inicial puede"
                    + " omitirse y se ignora.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "period-q1")
        @NotBlank
        String periodId,
    @Schema(
            description =
                "Sobre principal al que se atribuye el gasto. La cuenta de pago se deriva de este"
                    + " sobre.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "envelope-central")
        @NotBlank
        String targetEnvelopeId,
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
        String description,
    @Schema(
            description =
                "Financiación completa, incluida la aportación del principal si participa. null o"
                    + " [] usa el principal por el importe total. La suma debe ser exacta.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        List<@NotNull @Valid FundingRequest> fundingSources) {
  public Expense toCommand() {
    return new Expense(
        periodId,
        targetEnvelopeId,
        amount,
        description,
        fundingSources == null ? null : fundingSources.stream().map(x -> x.toDomain()).toList());
  }
}

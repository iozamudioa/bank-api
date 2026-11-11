package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.CreateEnvelope;
import dev.ioza.bank.domain.enums.EnvelopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "Crea un sobre con reserva cero y plan habitual opcional en una sola transaccion.",
    example =
        "{\"name\":\"Central de"
            + " Abastos\",\"accountId\":\"account-cash\",\"type\":\"SPENDING\",\"category\":\"Alimentación\",\"habitualAmount\":4500}")
public record CreateEnvelopeRequest(
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar")
        @NotBlank
        String name,
    @Schema(
            description = "Cuenta activa del mismo hogar. Determina dónde está el dinero real.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash")
        @NotBlank
        String accountId,
    @Schema(
            description =
                "Finalidad: SPENDING, SAVINGS o GOAL. Físico/digital depende de accountId.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "SPENDING")
        @NotNull
        EnvelopeType type,
    @Schema(
            description = "Categoría libre opcional.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            example = "Alimentación")
        String category,
    @Schema(
            description =
                "Planeado habitual por quincena. null u omitido: sin configurar; cero es explicito."
                    + " Crea o amplia la plantilla sin mover dinero ni alterar periodos"
                    + " existentes.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            type = "number",
            format = "decimal",
            example = "4500")
        @DecimalMin("0.00")
        @Digits(integer = 32, fraction = 2)
        BigDecimal habitualAmount) {
  public CreateEnvelope toCommand() {
    return new CreateEnvelope(name, accountId, type, category, habitualAmount);
  }
}

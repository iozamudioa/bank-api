package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.EnvelopePlan;
import dev.ioza.bank.domain.enums.EnvelopeType;
import dev.ioza.bank.domain.model.Envelope;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "EnvelopeResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record EnvelopeResponse(
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
            description = "Cuenta activa del mismo hogar. Determina dónde está el dinero real.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash",
            accessMode = Schema.AccessMode.READ_ONLY)
        String accountId,
    @Schema(
            description =
                "Finalidad: SPENDING, SAVINGS o GOAL. Físico/digital depende de accountId.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "SPENDING",
            accessMode = Schema.AccessMode.READ_ONLY)
        EnvelopeType type,
    @Schema(
            description = "Categoría libre opcional.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            example = "Alimentación",
            accessMode = Schema.AccessMode.READ_ONLY)
        String category,
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
        boolean active,
    @Schema(
            description =
                "Monto habitual derivado de la plantilla. null: no configurado; 0: plan explicito"
                    + " en cero. No es saldo reservado ni plan del periodo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            type = "number",
            format = "decimal",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal habitualAmount) {
  public static EnvelopeResponse from(EnvelopePlan plan) {
    return from(plan.envelope(), plan.habitualAmount());
  }

  public static EnvelopeResponse from(Envelope value) {
    return from(value, null);
  }

  private static EnvelopeResponse from(Envelope value, BigDecimal habitualAmount) {
    return new EnvelopeResponse(
        value.id(),
        value.householdId(),
        value.name(),
        value.accountId(),
        value.type(),
        value.category(),
        value.balance(),
        value.version(),
        value.active(),
        habitualAmount);
  }
}

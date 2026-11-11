package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.model.Movement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(
    description =
        "MovementResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record MovementResponse(
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
            description =
                "Quincena del movimiento; debe estar OPEN al confirmar. En saldo inicial puede"
                    + " omitirse y se ignora.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            example = "period-q1",
            accessMode = Schema.AccessMode.READ_ONLY)
        String periodId,
    @Schema(
            description = "Tipo del recurso según el enum del contrato.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "EXPENSE",
            accessMode = Schema.AccessMode.READ_ONLY)
        MovementType type,
    @Schema(
            description = "Estado persistido del recurso.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "POSTED",
            accessMode = Schema.AccessMode.READ_ONLY)
        MovementStatus status,
    @Schema(
            description =
                "Importe decimal MXN. Número JSON, máximo dos decimales y 32 dígitos enteros; sin"
                    + " redondeo implícito. Se serializa como número JSON decimal, nunca Extended"
                    + " JSON de MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "150",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal amount,
    @Schema(
            description = "Concepto opcional del movimiento.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            example = "Carne",
            accessMode = Schema.AccessMode.READ_ONLY)
        String description,
    @Schema(
            description =
                "Sobre principal al que se atribuye el gasto. La cuenta de pago se deriva de este"
                    + " sobre.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            example = "envelope-central",
            accessMode = Schema.AccessMode.READ_ONLY)
        String targetEnvelopeId,
    @Schema(
            description =
                "Fuente activa del hogar, opcional. No determina automáticamente la cuenta del"
                    + " ingreso.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            example = "income-payroll",
            accessMode = Schema.AccessMode.READ_ONLY)
        String incomeSourceId,
    @Schema(
            description = "Username autenticado que registró la operación.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "olda",
            accessMode = Schema.AccessMode.READ_ONLY)
        String actor,
    @Schema(
            description =
                "Instante UTC ISO-8601, precisión de milisegundos. No es la fecha local de la"
                    + " quincena.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-11-15T18:00:00Z",
            accessMode = Schema.AccessMode.READ_ONLY)
        Instant occurredAt,
    @Schema(
            description = "Identificador del original si type=REVERSAL; null en otros movimientos.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true,
            example = "movement-original",
            accessMode = Schema.AccessMode.READ_ONLY)
        String reversalOf,
    @Schema(
            description = "Aportaciones auditadas del gasto; no son movimientos adicionales.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
        List<FundingResponse> funding,
    @Schema(
            description =
                "Cambios auditados de cuentas reales, incluso efectos cero usados para proteger"
                    + " reservas.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
        List<BalanceEffectResponse> accountEffects,
    @Schema(
            description = "Cambios auditados de reservas.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY)
        List<BalanceEffectResponse> envelopeEffects,
    @Schema(
            description =
                "Presente en reemplazos de correccion; null en movimientos anteriores o"
                    + " independientes.",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY)
        CorrectionLinkResponse correction,
    @Schema(
            description =
                "Reemplazo confirmado con recursos explicitos; null para movimientos originales o"
                    + " correcciones antiguas sin este dato.",
            nullable = true,
            accessMode = Schema.AccessMode.READ_ONLY)
        MovementReplacementResponse replacement) {
  public static MovementResponse from(Movement value) {
    return new MovementResponse(
        value.id(),
        value.householdId(),
        value.periodId(),
        value.type(),
        value.status(),
        value.amount(),
        value.description(),
        value.targetEnvelopeId(),
        value.incomeSourceId(),
        value.actor(),
        value.occurredAt(),
        value.reversalOf(),
        value.funding().stream().map(FundingResponse::from).toList(),
        value.accountEffects().stream().map(BalanceEffectResponse::from).toList(),
        value.envelopeEffects().stream().map(BalanceEffectResponse::from).toList(),
        CorrectionLinkResponse.from(value.correction()),
        MovementReplacementResponse.from(value));
  }
}

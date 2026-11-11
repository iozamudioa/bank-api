package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.CorrectMovement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "Correccion atomica. Conserva tipo, historial y valida saldos netos. No admite saldo"
            + " inicial ni reversiones.",
    example =
        "{\"periodId\":\"period-q1\",\"reason\":\"Correccion de"
            + " captura\",\"replacement\":{\"type\":\"INCOME\",\"accountId\":\"account-cash\",\"amount\":1200,\"description\":\"Ingreso"
            + " corregido\"}}")
public record CorrectionRequest(
    @NotBlank String periodId,
    @NotBlank String reason,
    @NotNull @Valid MovementReplacementRequest replacement) {
  public CorrectMovement toCommand() {
    return new CorrectMovement(periodId, reason, replacement.toCommand());
  }
}

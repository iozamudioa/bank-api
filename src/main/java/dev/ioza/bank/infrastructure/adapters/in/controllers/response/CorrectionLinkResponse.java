package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.model.CorrectionLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description =
        "Vinculos y efectos NETOS confirmados de la correccion. Los efectos del movimiento"
            + " principal describen solo el reemplazo.")
public record CorrectionLinkResponse(
    String originalId,
    String reversalId,
    String reason,
    List<BalanceEffectResponse> netAccountEffects,
    List<BalanceEffectResponse> netEnvelopeEffects) {
  public static CorrectionLinkResponse from(CorrectionLink c) {
    return c == null
        ? null
        : new CorrectionLinkResponse(
            c.originalId(),
            c.reversalId(),
            c.reason(),
            c.netAccountEffects().stream().map(BalanceEffectResponse::from).toList(),
            c.netEnvelopeEffects().stream().map(BalanceEffectResponse::from).toList());
  }
}

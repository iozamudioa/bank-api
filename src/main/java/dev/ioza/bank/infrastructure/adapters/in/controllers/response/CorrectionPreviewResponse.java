package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.CorrectionPreview;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
    description =
        "Efectos NETOS con saldos actuales antes/cambio/despues; sin escrituras ni garantia de"
            + " disponibilidad posterior.")
public record CorrectionPreviewResponse(
    String originalId,
    List<BalanceEffectResponse> accountEffects,
    List<BalanceEffectResponse> envelopeEffects) {
  public static CorrectionPreviewResponse from(CorrectionPreview p) {
    return new CorrectionPreviewResponse(
        p.originalId(),
        p.accountEffects().stream().map(BalanceEffectResponse::from).toList(),
        p.envelopeEffects().stream().map(BalanceEffectResponse::from).toList());
  }
}

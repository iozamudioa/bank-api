package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import java.util.List;

public record CorrectionLinkDoc(
    String originalId,
    String reversalId,
    String reason,
    List<BalanceEffectDoc> netAccountEffects,
    List<BalanceEffectDoc> netEnvelopeEffects,
    ReplacementResourcesDoc replacement) {}

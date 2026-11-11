package dev.ioza.bank.domain.model;

import java.util.List;

/** Links and the committed net effects of an atomic correction. */
public record CorrectionLink(
    String originalId,
    String reversalId,
    String reason,
    List<BalanceEffect> netAccountEffects,
    List<BalanceEffect> netEnvelopeEffects,
    ReplacementResources replacement) {}

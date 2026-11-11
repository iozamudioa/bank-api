package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.domain.model.BalanceEffect;
import java.util.List;

public record CorrectionPreview(
    String originalId, List<BalanceEffect> accountEffects, List<BalanceEffect> envelopeEffects) {}

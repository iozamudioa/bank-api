package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.EnvelopeTransfer;
import dev.ioza.bank.domain.model.Movement;

public interface TransferEnvelopes {
  Movement transferEnvelope(String householdId, EnvelopeTransfer cmd);
}

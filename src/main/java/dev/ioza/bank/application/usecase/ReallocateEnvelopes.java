package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Reallocation;
import dev.ioza.bank.domain.model.Movement;

public interface ReallocateEnvelopes {
  Movement reallocate(String householdId, Reallocation cmd);
}

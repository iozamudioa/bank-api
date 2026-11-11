package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Allocation;
import dev.ioza.bank.domain.model.Movement;

public interface ReleaseReserve {
  Movement release(String householdId, Allocation cmd);
}

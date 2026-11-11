package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Allocation;
import dev.ioza.bank.domain.model.Movement;

public interface AllocateMoney {
  Movement allocate(String householdId, Allocation cmd);
}

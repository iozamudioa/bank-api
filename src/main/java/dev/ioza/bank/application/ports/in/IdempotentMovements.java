package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.domain.model.Movement;
import java.util.function.Supplier;

public interface IdempotentMovements {
  Movement execute(
      String householdId, String key, String operation, Object command, Supplier<Movement> action);
}

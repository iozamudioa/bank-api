package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Reverse;
import dev.ioza.bank.domain.model.Movement;

public interface ReverseMovement {
  Movement reverse(String householdId, String movementId, Reverse cmd);
}

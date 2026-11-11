package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.OpeningBalance;
import dev.ioza.bank.domain.model.Movement;

public interface RegisterOpeningBalance {
  Movement opening(String householdId, OpeningBalance cmd);
}

package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Income;
import dev.ioza.bank.domain.model.Movement;

public interface RegisterIncome {
  Movement income(String householdId, Income cmd);
}

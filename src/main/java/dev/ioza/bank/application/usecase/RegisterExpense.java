package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Expense;
import dev.ioza.bank.domain.model.Movement;

public interface RegisterExpense {
  Movement expense(String householdId, Expense cmd);
}

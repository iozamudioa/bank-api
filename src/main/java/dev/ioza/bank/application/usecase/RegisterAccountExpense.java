package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.AccountExpense;
import dev.ioza.bank.application.ports.in.AccountExpensePreview;
import dev.ioza.bank.domain.model.Movement;

public interface RegisterAccountExpense {
  Movement accountExpense(String householdId, AccountExpense command);

  AccountExpensePreview previewAccountExpense(String householdId, AccountExpense command);
}

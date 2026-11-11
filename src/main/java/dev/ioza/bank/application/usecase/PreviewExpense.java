package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Expense;
import dev.ioza.bank.application.ports.in.ExpensePreview;

public interface PreviewExpense {
  ExpensePreview previewExpense(String householdId, Expense cmd);
}

package dev.ioza.bank.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record BudgetTemplate(
    String id, String householdId, BigDecimal expectedIncome, List<BudgetLine> lines) {
  public BudgetTemplate withLine(String envelopeId, BigDecimal amount) {
    List<BudgetLine> updated = new ArrayList<>(lines);
    int index = -1;
    for (int i = 0; i < updated.size(); i++)
      if (updated.get(i).envelopeId().equals(envelopeId)) {
        index = i;
        break;
      }
    BudgetLine line = new BudgetLine(envelopeId, amount);
    if (index < 0) updated.add(line);
    else updated.set(index, line);
    return new BudgetTemplate(id, householdId, expectedIncome, List.copyOf(updated));
  }
}

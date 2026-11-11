package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.application.commands.CreatePeriod;
import dev.ioza.bank.application.commands.SaveBudget;
import dev.ioza.bank.application.commands.UpsertPeriodBudgetLine;
import dev.ioza.bank.application.commands.UpsertTemplateBudgetLine;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import java.util.List;

public interface Budget {
  BudgetTemplate saveTemplate(String householdId, SaveBudget cmd);

  BudgetTemplate template(String householdId);

  BudgetTemplate upsertTemplateBudgetLine(String householdId, UpsertTemplateBudgetLine cmd);

  BudgetPeriod createPeriod(String householdId, CreatePeriod cmd);

  BudgetPeriod getPeriod(String householdId, String periodId);

  List<BudgetPeriod> periods(String householdId);

  BudgetPeriod close(String householdId, String periodId);

  BudgetPeriod upsertPeriodBudgetLine(
      String householdId, String periodId, UpsertPeriodBudgetLine cmd);
}

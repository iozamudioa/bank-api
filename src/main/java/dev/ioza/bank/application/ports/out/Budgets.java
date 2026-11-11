package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Budgets {
  void lockPeriod(String periodId);

  void lockSchedule(String householdId);

  BudgetTemplate saveTemplate(BudgetTemplate value);

  Optional<BudgetTemplate> getTemplate(String householdId);

  BudgetPeriod savePeriod(BudgetPeriod value);

  Optional<BudgetPeriod> getPeriod(String id);

  Optional<BudgetPeriod> findStart(String householdId, LocalDate start);

  List<BudgetPeriod> periods(String householdId);
}

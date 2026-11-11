package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.IncomeSource;
import java.util.List;
import java.util.Optional;

public interface Incomes {
  IncomeSource save(IncomeSource value);

  Optional<IncomeSource> find(String id);

  List<IncomeSource> all(String householdId);
}

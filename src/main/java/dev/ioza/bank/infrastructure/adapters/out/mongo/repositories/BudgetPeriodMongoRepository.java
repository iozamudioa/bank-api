package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BudgetPeriodDoc;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BudgetPeriodMongoRepository extends MongoRepository<BudgetPeriodDoc, String> {
  List<BudgetPeriodDoc> findByHouseholdIdOrderByStartDateDesc(String householdId);

  Optional<BudgetPeriodDoc> findByHouseholdIdAndStartDate(String householdId, LocalDate start);
}

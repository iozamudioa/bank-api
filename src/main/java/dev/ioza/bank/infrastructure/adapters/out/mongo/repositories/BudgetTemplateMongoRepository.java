package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BudgetTemplateDoc;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BudgetTemplateMongoRepository extends MongoRepository<BudgetTemplateDoc, String> {
  Optional<BudgetTemplateDoc> findByHouseholdId(String householdId);
}

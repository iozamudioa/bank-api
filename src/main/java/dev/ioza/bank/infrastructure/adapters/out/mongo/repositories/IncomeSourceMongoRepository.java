package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.IncomeSourceDoc;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IncomeSourceMongoRepository extends MongoRepository<IncomeSourceDoc, String> {
  List<IncomeSourceDoc> findByHouseholdId(String householdId);
}

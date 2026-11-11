package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.MovementDoc;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MovementMongoRepository extends MongoRepository<MovementDoc, String> {
  List<MovementDoc> findTop100ByHouseholdIdOrderByOccurredAtDesc(String householdId);

  boolean existsByReversalOf(String reversalOf);
}

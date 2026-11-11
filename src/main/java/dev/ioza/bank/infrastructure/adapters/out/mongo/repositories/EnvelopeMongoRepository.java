package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.EnvelopeDoc;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EnvelopeMongoRepository extends MongoRepository<EnvelopeDoc, String> {
  List<EnvelopeDoc> findByHouseholdId(String householdId);

  List<EnvelopeDoc> findByHouseholdIdAndAccountId(String householdId, String accountId);
}

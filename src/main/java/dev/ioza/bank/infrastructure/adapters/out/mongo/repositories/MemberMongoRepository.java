package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.HouseholdMemberDoc;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MemberMongoRepository extends MongoRepository<HouseholdMemberDoc, String> {
  Optional<HouseholdMemberDoc> findByHouseholdIdAndUsername(String householdId, String username);

  List<HouseholdMemberDoc> findByHouseholdId(String householdId);
}

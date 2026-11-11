package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Members;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.MemberMongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MemberMongoAdapter implements Members {
  private final MemberMongoRepository db;

  public MemberMongoAdapter(MemberMongoRepository db) {
    this.db = db;
  }

  public HouseholdMember save(HouseholdMember x) {
    return MongoMapper.toDomain(db.save(MongoMapper.toDocument(x)));
  }

  public Optional<HouseholdMember> find(String householdId, String username) {
    return db.findByHouseholdIdAndUsername(householdId, username).map(MongoMapper::toDomain);
  }

  public List<HouseholdMember> all(String householdId) {
    return db.findByHouseholdId(householdId).stream().map(MongoMapper::toDomain).toList();
  }
}

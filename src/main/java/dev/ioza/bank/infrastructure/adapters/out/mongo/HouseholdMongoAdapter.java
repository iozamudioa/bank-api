package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Households;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.HouseholdMongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HouseholdMongoAdapter implements Households {
  private final HouseholdMongoRepository db;

  public HouseholdMongoAdapter(HouseholdMongoRepository db) {
    this.db = db;
  }

  public Household save(Household x) {
    return MongoMapper.toDomain(db.save(MongoMapper.toDocument(x)));
  }

  public Optional<Household> find(String id) {
    return db.findById(id).map(MongoMapper::toDomain);
  }

  public List<Household> all() {
    return db.findAll().stream().map(MongoMapper::toDomain).toList();
  }
}

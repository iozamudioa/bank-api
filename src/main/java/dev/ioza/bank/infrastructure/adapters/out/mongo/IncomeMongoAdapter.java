package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Incomes;
import dev.ioza.bank.domain.model.IncomeSource;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.IncomeSourceMongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class IncomeMongoAdapter implements Incomes {
  private final IncomeSourceMongoRepository db;

  public IncomeMongoAdapter(IncomeSourceMongoRepository db) {
    this.db = db;
  }

  public IncomeSource save(IncomeSource x) {
    return MongoMapper.toDomain(db.save(MongoMapper.toDocument(x)));
  }

  public Optional<IncomeSource> find(String id) {
    return db.findById(id).map(MongoMapper::toDomain);
  }

  public List<IncomeSource> all(String householdId) {
    return db.findByHouseholdId(householdId).stream().map(MongoMapper::toDomain).toList();
  }
}

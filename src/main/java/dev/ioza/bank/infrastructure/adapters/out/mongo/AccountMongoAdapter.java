package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.AccountMongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AccountMongoAdapter implements Accounts {
  private final AccountMongoRepository db;

  public AccountMongoAdapter(AccountMongoRepository db) {
    this.db = db;
  }

  public Account save(Account x) {
    return MongoMapper.toDomain(db.save(MongoMapper.toDocument(x)));
  }

  public Optional<Account> find(String id) {
    return db.findById(id).map(MongoMapper::toDomain);
  }

  public List<Account> all(String householdId) {
    return db.findByHouseholdId(householdId).stream().map(MongoMapper::toDomain).toList();
  }
}

package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.EnvelopeMongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EnvelopeMongoAdapter implements Envelopes {
  private final EnvelopeMongoRepository db;

  public EnvelopeMongoAdapter(EnvelopeMongoRepository db) {
    this.db = db;
  }

  public Envelope save(Envelope x) {
    return MongoMapper.toDomain(db.save(MongoMapper.toDocument(x)));
  }

  public Optional<Envelope> find(String id) {
    return db.findById(id).map(MongoMapper::toDomain);
  }

  public List<Envelope> all(String householdId) {
    return db.findByHouseholdId(householdId).stream().map(MongoMapper::toDomain).toList();
  }

  public List<Envelope> byAccount(String householdId, String accountId) {
    return db.findByHouseholdIdAndAccountId(householdId, accountId).stream()
        .map(MongoMapper::toDomain)
        .toList();
  }
}

package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.IdempotencyStore;
import dev.ioza.bank.domain.model.IdempotencyRecord;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.IdempotencyDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyMongoAdapter implements IdempotencyStore {
  private final MongoTemplate mongo;

  public IdempotencyMongoAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public Optional<IdempotencyRecord> find(String id) {
    return Optional.ofNullable(mongo.findById(id, IdempotencyDoc.class))
        .map(
            x ->
                new IdempotencyRecord(
                    x.id(), x.householdId(), x.fingerprint(), MongoMapper.toDomain(x.response())));
  }

  @Override
  public void insert(IdempotencyRecord record) {
    // insert, never upsert: the unique _id arbitrates simultaneous requests.
    mongo.insert(
        new IdempotencyDoc(
            record.id(),
            record.householdId(),
            record.fingerprint(),
            MongoMapper.toDocument(record.response())));
  }
}

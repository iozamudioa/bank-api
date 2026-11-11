package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.EnvelopeCreations;
import dev.ioza.bank.domain.model.EnvelopeCreationRecord;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.EnvelopeCreationDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class EnvelopeCreationMongoAdapter implements EnvelopeCreations {
  private final MongoTemplate mongo;

  public EnvelopeCreationMongoAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  public Optional<EnvelopeCreationRecord> find(String id) {
    return Optional.ofNullable(mongo.findById(id, EnvelopeCreationDoc.class))
        .map(
            d ->
                new EnvelopeCreationRecord(
                    d.id(),
                    d.householdId(),
                    d.fingerprint(),
                    MongoMapper.toDomain(d.envelope()),
                    d.habitualAmount()));
  }

  public void insert(EnvelopeCreationRecord r) {
    mongo.insert(
        new EnvelopeCreationDoc(
            r.id(),
            r.householdId(),
            r.fingerprint(),
            MongoMapper.toDocument(r.envelope()),
            r.habitualAmount()));
  }
}

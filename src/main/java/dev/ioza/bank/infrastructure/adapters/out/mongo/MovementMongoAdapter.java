package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Movements;
import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.model.Movement;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.MovementDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.MovementMongoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class MovementMongoAdapter implements Movements {
  private final MovementMongoRepository db;
  private final MongoTemplate mongo;

  public MovementMongoAdapter(MovementMongoRepository db, MongoTemplate mongo) {
    this.db = db;
    this.mongo = mongo;
  }

  public boolean hasOpening(String householdId, String accountId) {
    return mongo.exists(
        Query.query(
            Criteria.where("householdId")
                .is(householdId)
                .and("type")
                .is(MovementType.OPENING_BALANCE)
                .and("accountEffects.entityId")
                .is(accountId)),
        MovementDoc.class);
  }

  public List<Movement> search(String householdId, MovementQuery filter) {
    Criteria criteria = Criteria.where("householdId").is(householdId);
    if (filter.periodId() != null) criteria.and("periodId").is(filter.periodId());
    if (filter.accountId() != null) criteria.and("accountEffects.entityId").is(filter.accountId());
    if (filter.envelopeId() != null)
      criteria.and("envelopeEffects.entityId").is(filter.envelopeId());
    Query query =
        Query.query(criteria)
            .with(Sort.by(Sort.Direction.DESC, "occurredAt", "_id"))
            .skip((long) filter.page() * filter.size())
            .limit(filter.size());
    return mongo.find(query, MovementDoc.class).stream().map(MongoMapper::toDomain).toList();
  }

  public Movement save(Movement x) {
    return MongoMapper.toDomain(db.save(MongoMapper.toDocument(x)));
  }

  public Optional<Movement> find(String id) {
    return db.findById(id).map(MongoMapper::toDomain);
  }

  public List<Movement> recent(String householdId) {
    return db.findTop100ByHouseholdIdOrderByOccurredAtDesc(householdId).stream()
        .map(MongoMapper::toDomain)
        .toList();
  }

  public boolean hasReversal(String originalId) {
    return db.existsByReversalOf(originalId);
  }
}

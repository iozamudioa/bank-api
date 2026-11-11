package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.domain.model.Movement;
import java.util.List;
import java.util.Optional;

public interface Movements {
  List<Movement> search(String householdId, MovementQuery query);

  boolean hasOpening(String householdId, String accountId);

  Movement save(Movement value);

  Optional<Movement> find(String id);

  List<Movement> recent(String householdId);

  boolean hasReversal(String originalId);
}

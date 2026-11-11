package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyStore {
  Optional<IdempotencyRecord> find(String id);

  void insert(IdempotencyRecord record);
}

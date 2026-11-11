package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.EnvelopeCreationRecord;
import java.util.Optional;

public interface EnvelopeCreations {
  Optional<EnvelopeCreationRecord> find(String id);

  void insert(EnvelopeCreationRecord record);
}

package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.Envelope;
import java.util.List;
import java.util.Optional;

public interface Envelopes {
  Envelope save(Envelope value);

  Optional<Envelope> find(String id);

  List<Envelope> all(String householdId);

  List<Envelope> byAccount(String householdId, String accountId);
}

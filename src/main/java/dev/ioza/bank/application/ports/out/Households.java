package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.Household;
import java.util.List;
import java.util.Optional;

public interface Households {
  Household save(Household value);

  Optional<Household> find(String id);

  List<Household> all();
}

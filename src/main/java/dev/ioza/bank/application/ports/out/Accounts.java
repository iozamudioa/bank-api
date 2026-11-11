package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.Account;
import java.util.List;
import java.util.Optional;

public interface Accounts {
  Account save(Account value);

  Optional<Account> find(String id);

  List<Account> all(String householdId);
}

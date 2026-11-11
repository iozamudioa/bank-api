package dev.ioza.bank.application.ports.out;

import dev.ioza.bank.domain.model.HouseholdMember;
import java.util.List;
import java.util.Optional;

public interface Members {
  HouseholdMember save(HouseholdMember value);

  Optional<HouseholdMember> find(String householdId, String username);

  List<HouseholdMember> all(String householdId);
}

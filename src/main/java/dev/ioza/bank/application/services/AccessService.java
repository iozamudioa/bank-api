package dev.ioza.bank.application.services;

import dev.ioza.bank.application.ports.out.CurrentUser;
import dev.ioza.bank.application.ports.out.Households;
import dev.ioza.bank.application.ports.out.Members;
import dev.ioza.bank.domain.enums.MemberRole;
import dev.ioza.bank.domain.exceptions.ForbiddenException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.domain.model.HouseholdMember;
import org.springframework.stereotype.Service;

@Service
public class AccessService {
  private final Households households;
  private final Members members;
  private final CurrentUser currentUser;

  public AccessService(Households households, Members members, CurrentUser currentUser) {
    this.households = households;
    this.members = members;
    this.currentUser = currentUser;
  }

  public String username() {
    return currentUser.username();
  }

  public HouseholdMember requireMember(String householdId) {
    if (households.find(householdId).filter(Household::active).isEmpty())
      throw new NotFoundException("Hogar no encontrado");
    return members
        .find(householdId, username())
        .filter(HouseholdMember::active)
        .orElseThrow(() -> new ForbiddenException("No perteneces a este hogar"));
  }

  public void requireEditor(String householdId) {
    if (requireMember(householdId).role() == MemberRole.VIEWER)
      throw new ForbiddenException("Se requiere permiso de edición");
  }

  public void requireOwner(String householdId) {
    if (requireMember(householdId).role() != MemberRole.OWNER)
      throw new ForbiddenException("Se requiere permiso de propietario");
  }
}

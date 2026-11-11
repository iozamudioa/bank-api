package dev.ioza.bank.application.usecase;

import dev.ioza.bank.application.commands.Transfer;
import dev.ioza.bank.domain.model.Movement;

public interface TransferAccounts {
  Movement transfer(String householdId, Transfer cmd);
}

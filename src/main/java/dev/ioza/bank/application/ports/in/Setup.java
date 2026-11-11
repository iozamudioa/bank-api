package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.application.commands.AddMember;
import dev.ioza.bank.application.commands.CreateAccount;
import dev.ioza.bank.application.commands.CreateEnvelope;
import dev.ioza.bank.application.commands.CreateHousehold;
import dev.ioza.bank.application.commands.CreateIncomeSource;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.domain.model.IncomeSource;
import java.util.List;

public interface Setup {
  Household createHousehold(CreateHousehold cmd);

  Household household(String householdId);

  List<Household> households();

  HouseholdMember addMember(String householdId, AddMember cmd);

  List<HouseholdMember> members(String householdId);

  Account createAccount(String householdId, CreateAccount cmd);

  List<Account> accounts(String householdId);

  Envelope createEnvelope(String householdId, CreateEnvelope cmd);

  List<Envelope> envelopes(String householdId);

  EnvelopePlan createEnvelopeWithPlan(
      String householdId, CreateEnvelope cmd, String idempotencyKey);

  List<EnvelopePlan> envelopesWithPlans(String householdId);

  IncomeSource createIncomeSource(String householdId, CreateIncomeSource cmd);

  List<IncomeSource> incomeSources(String householdId);
}

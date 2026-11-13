package dev.ioza.bank.application.services;

import dev.ioza.bank.application.commands.AddMember;
import dev.ioza.bank.application.commands.CreateAccount;
import dev.ioza.bank.application.commands.CreateEnvelope;
import dev.ioza.bank.application.commands.CreateHousehold;
import dev.ioza.bank.application.commands.CreateIncomeSource;
import dev.ioza.bank.application.ports.in.EnvelopePlan;
import dev.ioza.bank.application.ports.in.Setup;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Budgets;
import dev.ioza.bank.application.ports.out.CommandFingerprint;
import dev.ioza.bank.application.ports.out.EnvelopeCreations;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.application.ports.out.Households;
import dev.ioza.bank.application.ports.out.Incomes;
import dev.ioza.bank.application.ports.out.Members;
import dev.ioza.bank.domain.Money;
import dev.ioza.bank.domain.enums.MemberRole;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.BudgetTemplate;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.EnvelopeCreationRecord;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.domain.model.IncomeSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupService implements Setup {
  private final Households households;
  private final Members members;
  private final Accounts accounts;
  private final Envelopes envelopes;
  private final Incomes incomes;
  private final AccessService access;
  private final Budgets budgets;
  private final EnvelopeCreations creations;
  private final CommandFingerprint fingerprints;
  private final FinancialNotificationPublisher notificationPublisher;

  public SetupService(
      Households households,
      Members members,
      Accounts accounts,
      Envelopes envelopes,
      Incomes incomes,
      AccessService access,
      Budgets budgets,
      EnvelopeCreations creations,
      CommandFingerprint fingerprints,
      FinancialNotificationPublisher notificationPublisher) {
    this.households = households;
    this.members = members;
    this.accounts = accounts;
    this.envelopes = envelopes;
    this.incomes = incomes;
    this.access = access;
    this.budgets = budgets;
    this.creations = creations;
    this.fingerprints = fingerprints;
    this.notificationPublisher = notificationPublisher;
  }

  private static String id() {
    return UUID.randomUUID().toString();
  }

  private static String required(String value, String field) {
    if (value == null || value.isBlank()) throw new ValidationException(field + " obligatorio");
    return value.trim();
  }

  @Override
  @Transactional
  public Household createHousehold(CreateHousehold cmd) {
    Household h =
        households.save(
            new Household(
                id(), required(cmd.name(), "Nombre"), "MXN", "America/Mexico_City", true));
    members.save(
        new HouseholdMember(
            id(),
            h.id(),
            access.username(),
            required(cmd.displayName(), "Nombre visible"),
            MemberRole.OWNER,
            true));
    return h;
  }

  @Override
  public Household household(String householdId) {
    access.requireMember(householdId);
    return households.find(householdId).orElseThrow();
  }

  @Override
  public List<Household> households() {
    return households.all().stream()
        .filter(
            h ->
                members.find(h.id(), access.username()).filter(HouseholdMember::active).isPresent())
        .toList();
  }

  @Override
  @Transactional
  public HouseholdMember addMember(String householdId, AddMember cmd) {
    access.requireOwner(householdId);
    String username = required(cmd.username(), "Usuario");
    if (members.find(householdId, username).isPresent())
      throw new ConflictException("El usuario ya pertenece al hogar");
    if (cmd.role() == MemberRole.OWNER)
      throw new ValidationException("No se puede invitar como propietario");
    return members.save(
        new HouseholdMember(
            id(),
            householdId,
            username,
            required(cmd.displayName(), "Nombre visible"),
            cmd.role() == null ? MemberRole.EDITOR : cmd.role(),
            true));
  }

  @Override
  public List<HouseholdMember> members(String householdId) {
    access.requireMember(householdId);
    return members.all(householdId);
  }

  @Override
  public Account createAccount(String householdId, CreateAccount cmd) {
    access.requireEditor(householdId);
    if (cmd.type() == null) throw new ValidationException("Tipo de cuenta obligatorio");
    return accounts.save(
        new Account(
            id(), householdId, required(cmd.name(), "Nombre"), cmd.type(), Money.ZERO, null, true));
  }

  @Override
  public List<Account> accounts(String householdId) {
    access.requireMember(householdId);
    return accounts.all(householdId);
  }

  @Override
  @Transactional
  public Envelope createEnvelope(String householdId, CreateEnvelope cmd) {
    access.requireEditor(householdId);
    BigDecimal habitual =
        cmd.habitualAmount() == null ? null : Money.normalized(cmd.habitualAmount());
    if (habitual != null) {
      Money.nonNegative(habitual, "presupuesto habitual");
      budgets.lockSchedule(householdId);
    }
    Account account =
        accounts
            .find(cmd.accountId())
            .orElseThrow(() -> new NotFoundException("Cuenta no encontrada"));
    if (!account.householdId().equals(householdId) || !account.active())
      throw new ValidationException("Cuenta no disponible");
    if (cmd.type() == null) throw new ValidationException("Tipo de sobre obligatorio");
    Envelope envelope =
        envelopes.save(
            new Envelope(
                id(),
                householdId,
                required(cmd.name(), "Nombre"),
                account.id(),
                cmd.type(),
                cmd.category(),
                Money.ZERO,
                null,
                true));
    if (habitual != null) {
      BudgetTemplate template =
          budgets
              .getTemplate(householdId)
              .orElseGet(() -> new BudgetTemplate(id(), householdId, Money.ZERO, List.of()));
      budgets.saveTemplate(template.withLine(envelope.id(), habitual));
    }
    notificationPublisher.envelopeCreated(envelope, access.username());
    return envelope;
  }

  @Override
  public List<Envelope> envelopes(String householdId) {
    access.requireMember(householdId);
    return envelopes.all(householdId);
  }

  @Override
  @Transactional
  public EnvelopePlan createEnvelopeWithPlan(String householdId, CreateEnvelope cmd, String key) {
    access.requireEditor(householdId);
    String recordId = null, fingerprint = null;
    if (key != null) {
      if (key.isBlank() || key.length() > 128)
        throw new ValidationException("Idempotency-Key debe contener entre 1 y 128 caracteres");
      recordId = fingerprints.fingerprint("envelope-create-key", List.of(householdId, key));
      fingerprint = fingerprints.fingerprint("envelope-create", cmd);
      var prior = creations.find(recordId).orElse(null);
      if (prior != null) {
        if (!prior.fingerprint().equals(fingerprint))
          throw new ConflictException("Idempotency-Key ya utilizada con otro contenido");
        return new EnvelopePlan(prior.envelope(), prior.habitualAmount());
      }
    }
    Envelope envelope = createEnvelope(householdId, cmd);
    BigDecimal habitual =
        cmd.habitualAmount() == null ? null : Money.normalized(cmd.habitualAmount());
    if (recordId != null)
      creations.insert(
          new EnvelopeCreationRecord(recordId, householdId, fingerprint, envelope, habitual));
    return new EnvelopePlan(envelope, habitual);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnvelopePlan> envelopesWithPlans(String householdId) {
    access.requireMember(householdId);
    Map<String, BigDecimal> amounts = new HashMap<>();
    budgets
        .getTemplate(householdId)
        .ifPresent(t -> t.lines().forEach(line -> amounts.put(line.envelopeId(), line.amount())));
    return envelopes.all(householdId).stream()
        .map(e -> new EnvelopePlan(e, amounts.get(e.id())))
        .toList();
  }

  @Override
  public IncomeSource createIncomeSource(String householdId, CreateIncomeSource cmd) {
    access.requireEditor(householdId);
    Account account =
        accounts
            .find(cmd.accountId())
            .orElseThrow(() -> new NotFoundException("Cuenta no encontrada"));
    if (!account.householdId().equals(householdId) || !account.active())
      throw new ValidationException("Cuenta no disponible");
    if (cmd.type() == null) throw new ValidationException("Tipo de ingreso obligatorio");
    if (cmd.expectedAmount() != null)
      Money.nonNegative(Money.normalized(cmd.expectedAmount()), "ingreso esperado");
    return incomes.save(
        new IncomeSource(
            id(),
            householdId,
            required(cmd.name(), "Nombre"),
            cmd.type(),
            account.id(),
            cmd.expectedAmount() == null ? null : Money.normalized(cmd.expectedAmount()),
            true));
  }

  @Override
  public List<IncomeSource> incomeSources(String householdId) {
    access.requireMember(householdId);
    return incomes.all(householdId);
  }
}

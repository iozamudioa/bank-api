package dev.ioza.bank.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ioza.bank.application.commands.CreateAccount;
import dev.ioza.bank.application.commands.CreateEnvelope;
import dev.ioza.bank.application.commands.CreateHousehold;
import dev.ioza.bank.application.commands.CreateIncomeSource;
import dev.ioza.bank.application.commands.CreatePeriod;
import dev.ioza.bank.application.commands.SaveBudget;
import dev.ioza.bank.application.commands.UpsertPeriodBudgetLine;
import dev.ioza.bank.application.commands.UpsertTemplateBudgetLine;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Budgets;
import dev.ioza.bank.application.ports.out.EnvelopeCreations;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.application.ports.out.Households;
import dev.ioza.bank.application.ports.out.Incomes;
import dev.ioza.bank.application.ports.out.Members;
import dev.ioza.bank.domain.enums.AccountType;
import dev.ioza.bank.domain.enums.EnvelopeType;
import dev.ioza.bank.domain.enums.IncomeSourceType;
import dev.ioza.bank.domain.enums.MemberRole;
import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.ForbiddenException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.BudgetLine;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.EnvelopeCreationRecord;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.domain.model.IncomeSource;
import dev.ioza.bank.infrastructure.adapters.in.controllers.BudgetController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.EnvelopeController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.advice.ApiExceptionHandler;
import dev.ioza.bank.infrastructure.adapters.out.mongo.JsonCommandFingerprint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/**
 * In-memory catalogs test business rules; transaction guarantees are covered by
 * MongoFinanceIntegrationTest.
 */
class SetupBudgetServiceTest {
  final Map<String, Household> householdData = new HashMap<>();
  final Map<String, HouseholdMember> memberData = new HashMap<>();
  final Map<String, Account> accountData = new HashMap<>();
  final Map<String, Envelope> envelopeData = new HashMap<>();
  final Map<String, BudgetTemplate> templateData = new HashMap<>();
  final Map<String, BudgetPeriod> periodData = new HashMap<>();
  final Households households =
      new Households() {
        public Household save(Household x) {
          householdData.put(x.id(), x);
          return x;
        }

        public Optional<Household> find(String id) {
          return Optional.ofNullable(householdData.get(id));
        }

        public List<Household> all() {
          return List.copyOf(householdData.values());
        }
      };
  final Members members =
      new Members() {
        public HouseholdMember save(HouseholdMember x) {
          memberData.put(x.householdId() + ":" + x.username(), x);
          return x;
        }

        public Optional<HouseholdMember> find(String h, String u) {
          return Optional.ofNullable(memberData.get(h + ":" + u));
        }

        public List<HouseholdMember> all(String h) {
          return memberData.values().stream().filter(x -> x.householdId().equals(h)).toList();
        }
      };
  final Accounts accounts =
      new Accounts() {
        public Account save(Account x) {
          accountData.put(x.id(), x);
          return x;
        }

        public Optional<Account> find(String id) {
          return Optional.ofNullable(accountData.get(id));
        }

        public List<Account> all(String h) {
          return accountData.values().stream().filter(x -> x.householdId().equals(h)).toList();
        }
      };
  final Envelopes envelopes =
      new Envelopes() {
        public Envelope save(Envelope x) {
          envelopeData.put(x.id(), x);
          return x;
        }

        public Optional<Envelope> find(String id) {
          return Optional.ofNullable(envelopeData.get(id));
        }

        public List<Envelope> all(String h) {
          return envelopeData.values().stream().filter(x -> x.householdId().equals(h)).toList();
        }

        public List<Envelope> byAccount(String h, String a) {
          return all(h).stream().filter(x -> x.accountId().equals(a)).toList();
        }
      };
  final Incomes incomes =
      new Incomes() {
        public IncomeSource save(IncomeSource x) {
          return x;
        }

        public Optional<IncomeSource> find(String id) {
          return Optional.empty();
        }

        public List<IncomeSource> all(String h) {
          return List.of();
        }
      };
  final Budgets budgets =
      new Budgets() {
        public void lockPeriod(String id) {}

        public void lockSchedule(String id) {}

        public BudgetTemplate saveTemplate(BudgetTemplate x) {
          templateData.put(x.householdId(), x);
          return x;
        }

        public Optional<BudgetTemplate> getTemplate(String h) {
          return Optional.ofNullable(templateData.get(h));
        }

        public BudgetPeriod savePeriod(BudgetPeriod x) {
          periodData.put(x.id(), x);
          return x;
        }

        public Optional<BudgetPeriod> getPeriod(String id) {
          return Optional.ofNullable(periodData.get(id));
        }

        public Optional<BudgetPeriod> findStart(String h, LocalDate d) {
          return periods(h).stream().filter(x -> x.startDate().equals(d)).findFirst();
        }

        public List<BudgetPeriod> periods(String h) {
          return periodData.values().stream().filter(x -> x.householdId().equals(h)).toList();
        }
      };
  final AccessService access = new AccessService(households, members, () -> "olda");
  final Map<String, EnvelopeCreationRecord> creationData = new HashMap<>();
  final EnvelopeCreations creations =
      new EnvelopeCreations() {
        public Optional<EnvelopeCreationRecord> find(String id) {
          return Optional.ofNullable(creationData.get(id));
        }

        public void insert(EnvelopeCreationRecord r) {
          if (creationData.putIfAbsent(r.id(), r) != null) throw new ConflictException("Duplicado");
        }
      };
  final SetupService setup =
      new SetupService(
          households,
          members,
          accounts,
          envelopes,
          incomes,
          access,
          budgets,
          creations,
          new JsonCommandFingerprint(JsonMapper.builder().build()));
  final BudgetService budget = new BudgetService(budgets, envelopes, access, accounts);

  String household() {
    return setup.createHousehold(new CreateHousehold("Familia", "Olda")).id();
  }

  @Test
  void createsHouseholdOwnerAndEmptyAccountsAndEnvelopes() {
    String h = household();
    assertEquals(MemberRole.OWNER, members.find(h, "olda").orElseThrow().role());
    assertEquals("MXN", setup.household(h).currency());
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Ahorro", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    assertEquals(0, a.balance().signum());
    assertEquals(0, e.balance().signum());
    assertEquals(a.id(), e.accountId());
  }

  @Test
  void rejectsCrossHouseholdReferencesAndNegativeExpectedIncome() {
    String first = household(), second = household();
    var account = setup.createAccount(first, new CreateAccount("Banco", AccountType.DIGITAL));
    assertThrows(
        ValidationException.class,
        () ->
            setup.createEnvelope(
                second, new CreateEnvelope("Otro", account.id(), EnvelopeType.SAVINGS, "Ahorro")));
    assertThrows(
        ValidationException.class,
        () ->
            setup.createIncomeSource(
                first,
                new CreateIncomeSource(
                    "Pago", IncomeSourceType.PAYROLL, account.id(), new BigDecimal("-1.00"))));
  }

  @Test
  void periodSnapshotsRemainIndependentAndBalancesCarryOver() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Ahorro", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    accounts.save(a.withBalance(new BigDecimal("80.00")));
    envelopes.save(e.withBalance(new BigDecimal("80.00")));
    budget.saveTemplate(
        h,
        new SaveBudget(
            new BigDecimal("100.00"), List.of(new BudgetLine(e.id(), new BigDecimal("90.00")))));
    var p =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true));
    budget.close(h, p.id());
    budget.saveTemplate(h, new SaveBudget(new BigDecimal("1.00"), List.of()));
    budget.createPeriod(
        h, new CreatePeriod(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 30), true));
    assertEquals(1, budget.getPeriod(h, p.id()).lines().size());
    assertEquals(
        0, envelopes.find(e.id()).orElseThrow().balance().compareTo(new BigDecimal("80.00")));
    assertThrows(
        ConflictException.class,
        () ->
            budget.createPeriod(
                h, new CreatePeriod(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 20), true)));
  }

  @Test
  void viewerCannotCreateAccountsAndInactiveMembershipHasNoAccess() {
    String h = household();
    members.save(new HouseholdMember("viewer", h, "olda", "Olda", MemberRole.VIEWER, true));
    assertThrows(
        ForbiddenException.class,
        () -> setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL)));
    members.save(new HouseholdMember("viewer", h, "olda", "Olda", MemberRole.VIEWER, false));
    assertThrows(ForbiddenException.class, () -> setup.household(h));
  }

  @Test
  void addsAndReplacesPeriodPlanWithoutChangingTemplateOtherPeriodsOrBalances() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Original", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    accounts.save(a.withBalance(new BigDecimal("80.00")));
    envelopes.save(e.withBalance(new BigDecimal("30.00")));
    var template =
        budget.saveTemplate(
            h,
            new SaveBudget(
                new BigDecimal("100.00"),
                List.of(new BudgetLine(e.id(), new BigDecimal("90.00")))));
    var p =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true));
    var other =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 30), true));
    var added =
        setup.createEnvelope(
            h, new CreateEnvelope("Nuevo", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    var updated =
        budget.upsertPeriodBudgetLine(
            h, p.id(), new UpsertPeriodBudgetLine(added.id(), new BigDecimal("1500")));
    assertEquals(new BigDecimal("1500.00"), updated.lines().get(1).amount());
    for (int i = 0; i < 2; i++)
      updated =
          budget.upsertPeriodBudgetLine(
              h, p.id(), new UpsertPeriodBudgetLine(added.id(), BigDecimal.ZERO));
    assertEquals(
        List.of(p.lines().get(0), new BudgetLine(added.id(), new BigDecimal("0.00"))),
        updated.lines());
    assertEquals(
        new BudgetPeriod(
            p.id(), h, p.startDate(), p.endDate(), p.status(), p.expectedIncome(), updated.lines()),
        updated);
    assertEquals(template, budget.template(h));
    assertEquals(other, budget.getPeriod(h, other.id()));
    assertEquals(new BigDecimal("80.00"), accounts.find(a.id()).orElseThrow().balance());
    assertEquals(new BigDecimal("30.00"), envelopes.find(e.id()).orElseThrow().balance());
    assertEquals(added, envelopes.find(added.id()).orElseThrow());
  }

  @Test
  void rejectsInvalidAmountsAndUnavailableResourcesForPeriodPlan() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Sobre", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    budget.saveTemplate(h, new SaveBudget(BigDecimal.ZERO, List.of()));
    var p =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true));
    for (BigDecimal amount :
        new BigDecimal[] {
          null, new BigDecimal("-1"), new BigDecimal("1.001"), new BigDecimal("1E33")
        })
      assertThrows(
          ValidationException.class,
          () ->
              budget.upsertPeriodBudgetLine(h, p.id(), new UpsertPeriodBudgetLine(e.id(), amount)));
    assertThrows(
        NotFoundException.class,
        () ->
            budget.upsertPeriodBudgetLine(
                h, p.id(), new UpsertPeriodBudgetLine("missing", BigDecimal.ONE)));
    assertThrows(
        NotFoundException.class,
        () ->
            budget.upsertPeriodBudgetLine(
                household(), p.id(), new UpsertPeriodBudgetLine(e.id(), BigDecimal.ONE)));
    for (boolean active : new boolean[] {true, false}) {
      envelopeData.put(
          e.id(),
          new Envelope(
              e.id(),
              active ? "other" : h,
              e.name(),
              a.id(),
              e.type(),
              e.category(),
              e.balance(),
              e.version(),
              active));
      assertThrows(
          ValidationException.class,
          () ->
              budget.upsertPeriodBudgetLine(
                  h, p.id(), new UpsertPeriodBudgetLine(e.id(), BigDecimal.ONE)));
    }
    envelopeData.put(e.id(), e);
    for (boolean active : new boolean[] {true, false}) {
      accountData.put(
          a.id(),
          new Account(
              a.id(), active ? "other" : h, a.name(), a.type(), a.balance(), a.version(), active));
      assertThrows(
          ValidationException.class,
          () ->
              budget.upsertPeriodBudgetLine(
                  h, p.id(), new UpsertPeriodBudgetLine(e.id(), BigDecimal.ONE)));
    }
    accountData.remove(a.id());
    assertThrows(
        NotFoundException.class,
        () ->
            budget.upsertPeriodBudgetLine(
                h, p.id(), new UpsertPeriodBudgetLine(e.id(), BigDecimal.ONE)));
    assertEquals(p, budget.getPeriod(h, p.id()));
  }

  @Test
  void onlyEditorsAndOpenPeriodsCanChangePeriodPlan() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Sobre", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    budget.saveTemplate(h, new SaveBudget(BigDecimal.ZERO, List.of()));
    var p =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true));
    var cmd = new UpsertPeriodBudgetLine(e.id(), BigDecimal.ONE);
    members.save(new HouseholdMember("viewer", h, "olda", "Olda", MemberRole.VIEWER, true));
    assertThrows(ForbiddenException.class, () -> budget.upsertPeriodBudgetLine(h, p.id(), cmd));
    members.save(new HouseholdMember("editor", h, "olda", "Olda", MemberRole.EDITOR, false));
    assertThrows(ForbiddenException.class, () -> budget.upsertPeriodBudgetLine(h, p.id(), cmd));
    members.save(new HouseholdMember("editor", h, "olda", "Olda", MemberRole.EDITOR, true));
    budget.upsertPeriodBudgetLine(h, p.id(), cmd);
    budget.close(h, p.id());
    assertThrows(ConflictException.class, () -> budget.upsertPeriodBudgetLine(h, p.id(), cmd));
    periodData.put(
        p.id(),
        new BudgetPeriod(
            p.id(),
            h,
            p.startDate(),
            p.endDate(),
            PeriodStatus.DRAFT,
            p.expectedIncome(),
            p.lines()));
    assertThrows(ConflictException.class, () -> budget.upsertPeriodBudgetLine(h, p.id(), cmd));
  }

  @Test
  void creatingEnvelopeWithHabitualPlanCreatesFirstTemplateWithoutPeriodOrMoney() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var created =
        setup.createEnvelopeWithPlan(
            h,
            new CreateEnvelope(
                "Central", a.id(), EnvelopeType.SPENDING, "Comida", new BigDecimal("4500")),
            "first");
    assertEquals(new BigDecimal("4500.00"), created.habitualAmount());
    assertEquals(new BigDecimal("0.00"), created.envelope().balance());
    var template = budget.template(h);
    assertEquals(new BigDecimal("0.00"), template.expectedIncome());
    assertEquals(
        List.of(new BudgetLine(created.envelope().id(), new BigDecimal("4500.00"))),
        template.lines());
    assertEquals(new BigDecimal("0.00"), accounts.find(a.id()).orElseThrow().balance());
    assertTrue(periodData.isEmpty());
    var period =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true));
    assertEquals(template.lines(), period.lines());
    assertEquals(new BigDecimal("0.00"), period.expectedIncome());
    budget.upsertTemplateBudgetLine(
        h, new UpsertTemplateBudgetLine(created.envelope().id(), new BigDecimal("4800")));
    assertEquals(template.lines(), budget.getPeriod(h, period.id()).lines());
    assertEquals(new BigDecimal("4800.00"), setup.envelopesWithPlans(h).get(0).habitualAmount());
  }

  @Test
  void habitualCreationAndUpdatePreserveOtherLinesExpectedIncomeAndBalances() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var original =
        setup.createEnvelope(
            h, new CreateEnvelope("Original", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    var template =
        budget.saveTemplate(
            h,
            new SaveBudget(
                new BigDecimal("9000.00"),
                List.of(new BudgetLine(original.id(), new BigDecimal("1000.00")))));
    accounts.save(a.withBalance(new BigDecimal("500.00")));
    envelopes.save(original.withBalance(new BigDecimal("200.00")));
    var created =
        setup.createEnvelopeWithPlan(
            h,
            new CreateEnvelope(
                "Nuevo", a.id(), EnvelopeType.SPENDING, "Comida", new BigDecimal("4500.00")),
            null);
    for (int i = 0; i < 2; i++)
      budget.upsertTemplateBudgetLine(
          h, new UpsertTemplateBudgetLine(created.envelope().id(), new BigDecimal("4800.00")));
    var updated = budget.template(h);
    assertEquals(template.id(), updated.id());
    assertEquals(template.expectedIncome(), updated.expectedIncome());
    assertEquals(
        List.of(
            template.lines().get(0),
            new BudgetLine(created.envelope().id(), new BigDecimal("4800.00"))),
        updated.lines());
    assertEquals(new BigDecimal("500.00"), accounts.find(a.id()).orElseThrow().balance());
    assertEquals(new BigDecimal("200.00"), envelopes.find(original.id()).orElseThrow().balance());
    assertTrue(periodData.isEmpty());
  }

  @Test
  void nullHabitualDoesNotCreateTemplateAndExplicitZeroCreatesARealLine() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var legacy =
        setup.createEnvelopeWithPlan(
            h, new CreateEnvelope("Anterior", a.id(), EnvelopeType.SAVINGS, "Ahorro"), null);
    assertNull(legacy.habitualAmount());
    assertNull(setup.envelopesWithPlans(h).get(0).habitualAmount());
    assertTrue(templateData.isEmpty());
    var zero =
        setup.createEnvelopeWithPlan(
            h,
            new CreateEnvelope("Cero", a.id(), EnvelopeType.SAVINGS, "Ahorro", BigDecimal.ZERO),
            null);
    assertEquals(
        List.of(new BudgetLine(zero.envelope().id(), new BigDecimal("0.00"))),
        budget.template(h).lines());
    budget.upsertTemplateBudgetLine(
        h, new UpsertTemplateBudgetLine(legacy.envelope().id(), BigDecimal.ZERO));
    assertEquals(2, budget.template(h).lines().size());
    assertTrue(
        setup.envelopesWithPlans(h).stream()
            .allMatch(p -> new BigDecimal("0.00").equals(p.habitualAmount())));
    budget.saveTemplate(h, new SaveBudget(BigDecimal.ZERO, List.of()));
    assertTrue(setup.envelopesWithPlans(h).stream().allMatch(p -> p.habitualAmount() == null));
  }

  @Test
  void habitualLineUpdateCreatesFirstTemplateForExistingEnvelope() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Anterior", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    var template =
        budget.upsertTemplateBudgetLine(
            h, new UpsertTemplateBudgetLine(e.id(), new BigDecimal("750")));
    assertEquals(new BigDecimal("0.00"), template.expectedIncome());
    assertEquals(List.of(new BudgetLine(e.id(), new BigDecimal("750.00"))), template.lines());
    assertEquals(new BigDecimal("750.00"), setup.envelopesWithPlans(h).get(0).habitualAmount());
    assertTrue(periodData.isEmpty());
  }

  @Test
  void creationReplayKeepsOriginalResponseWithoutRestoringOldTemplateAndRequiresPermission() {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var cmd =
        new CreateEnvelope(
            "Nuevo", a.id(), EnvelopeType.SAVINGS, "Ahorro", new BigDecimal("750.00"));
    var created = setup.createEnvelopeWithPlan(h, cmd, "same");
    budget.upsertTemplateBudgetLine(
        h, new UpsertTemplateBudgetLine(created.envelope().id(), new BigDecimal("900.00")));
    assertEquals(created, setup.createEnvelopeWithPlan(h, cmd, "same"));
    assertEquals(1, envelopeData.size());
    assertEquals(1, creationData.size());
    assertEquals(new BigDecimal("900.00"), budget.template(h).lines().get(0).amount());
    assertThrows(
        ConflictException.class,
        () ->
            setup.createEnvelopeWithPlan(
                h,
                new CreateEnvelope(
                    "Otro", a.id(), EnvelopeType.SAVINGS, "Ahorro", new BigDecimal("750.00")),
                "same"));
    for (String key : List.of(" ", "x".repeat(129)))
      assertThrows(ValidationException.class, () -> setup.createEnvelopeWithPlan(h, cmd, key));
    members.save(new HouseholdMember("viewer", h, "olda", "Olda", MemberRole.VIEWER, true));
    assertThrows(ForbiddenException.class, () -> setup.createEnvelopeWithPlan(h, cmd, "same"));
    assertThrows(
        ForbiddenException.class,
        () ->
            budget.upsertTemplateBudgetLine(
                h, new UpsertTemplateBudgetLine(created.envelope().id(), BigDecimal.ONE)));
    members.save(new HouseholdMember("editor", h, "olda", "Olda", MemberRole.EDITOR, false));
    assertThrows(ForbiddenException.class, () -> setup.createEnvelopeWithPlan(h, cmd, "same"));
    members.save(new HouseholdMember("editor", h, "olda", "Olda", MemberRole.EDITOR, true));
    budget.upsertTemplateBudgetLine(
        h, new UpsertTemplateBudgetLine(created.envelope().id(), BigDecimal.ZERO));
  }

  @Test
  void invalidHabitualAmountsAndUnavailableReferencesLeaveCatalogAndPlanUnchanged() {
    String h = household(), other = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        setup.createEnvelope(
            h, new CreateEnvelope("Anterior", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    for (BigDecimal amount :
        new BigDecimal[] {new BigDecimal("-1"), new BigDecimal("1.001"), new BigDecimal("1E33")}) {
      assertThrows(
          ValidationException.class,
          () ->
              setup.createEnvelopeWithPlan(
                  h,
                  new CreateEnvelope("Invalido", a.id(), EnvelopeType.SAVINGS, "Ahorro", amount),
                  "invalid"));
      assertThrows(
          ValidationException.class,
          () -> budget.upsertTemplateBudgetLine(h, new UpsertTemplateBudgetLine(e.id(), amount)));
    }
    assertThrows(
        ValidationException.class,
        () -> budget.upsertTemplateBudgetLine(h, new UpsertTemplateBudgetLine(e.id(), null)));
    assertThrows(
        ValidationException.class,
        () ->
            setup.createEnvelopeWithPlan(
                other,
                new CreateEnvelope("Ajeno", a.id(), EnvelopeType.SAVINGS, "Ahorro", BigDecimal.ONE),
                null));
    assertThrows(
        ValidationException.class,
        () ->
            budget.upsertTemplateBudgetLine(
                other, new UpsertTemplateBudgetLine(e.id(), BigDecimal.ONE)));
    assertThrows(
        NotFoundException.class,
        () ->
            budget.upsertTemplateBudgetLine(
                h, new UpsertTemplateBudgetLine("missing", BigDecimal.ONE)));
    envelopeData.put(
        e.id(),
        new Envelope(
            e.id(), h, e.name(), a.id(), e.type(), e.category(), e.balance(), e.version(), false));
    assertThrows(
        ValidationException.class,
        () ->
            budget.upsertTemplateBudgetLine(
                h, new UpsertTemplateBudgetLine(e.id(), BigDecimal.ONE)));
    envelopeData.put(e.id(), e);
    for (String owner : List.of(h, other)) {
      accountData.put(
          a.id(), new Account(a.id(), owner, a.name(), a.type(), a.balance(), a.version(), false));
      assertThrows(
          ValidationException.class,
          () ->
              budget.upsertTemplateBudgetLine(
                  h, new UpsertTemplateBudgetLine(e.id(), BigDecimal.ONE)));
      assertThrows(
          ValidationException.class,
          () ->
              setup.createEnvelopeWithPlan(
                  h,
                  new CreateEnvelope(
                      "Invalido", a.id(), EnvelopeType.SAVINGS, "Ahorro", BigDecimal.ONE),
                  null));
    }
    accountData.put(
        a.id(), new Account(a.id(), other, a.name(), a.type(), a.balance(), a.version(), true));
    assertThrows(
        ValidationException.class,
        () ->
            budget.upsertTemplateBudgetLine(
                h, new UpsertTemplateBudgetLine(e.id(), BigDecimal.ONE)));
    assertEquals(1, envelopeData.size());
    assertTrue(templateData.isEmpty());
    assertTrue(creationData.isEmpty());
  }

  @Test
  void habitualPlanHttpIsOptionalCompatibleNumericAndIdempotent() throws Exception {
    String h = household();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var mvc =
        MockMvcBuilders.standaloneSetup(new EnvelopeController(setup), new BudgetController(budget))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    String url = "/api/v1/households/" + h + "/envelopes";
    String body =
        "{\"name\":\"Nuevo\",\"accountId\":\""
            + a.id()
            + "\",\"type\":\"SAVINGS\",\"habitualAmount\":750}";
    String response =
        mvc.perform(
                post(url)
                    .header("Idempotency-Key", "http")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.habitualAmount").value(750))
            .andExpect(jsonPath("$.balance").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(
        response,
        mvc.perform(
                post(url)
                    .header("Idempotency-Key", "http")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
    String id = envelopeData.keySet().iterator().next();
    String line = "/api/v1/households/" + h + "/budget-template/lines/" + id;
    mvc.perform(put(line).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lines[0].amount").value(0));
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].habitualAmount").value(0));
    for (String invalid : List.of("-1", "1.001", "100000000000000000000000000000000")) {
      mvc.perform(
              post(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body.replace("750", invalid)))
          .andExpect(status().isBadRequest());
      mvc.perform(
              put(line)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amount\":" + invalid + "}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(put(line).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(url)
                .header("Idempotency-Key", "http")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("750", "900")))
        .andExpect(status().isConflict());
    mvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace(",\"habitualAmount\":750", "")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.habitualAmount").isEmpty());
    assertEquals(2, envelopeData.size());
    assertEquals(1, budget.template(h).lines().size());
  }
}

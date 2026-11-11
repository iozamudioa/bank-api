package dev.ioza.bank.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ioza.bank.application.commands.AccountExpense;
import dev.ioza.bank.application.commands.Allocation;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.MovementResponse;
import dev.ioza.bank.application.commands.CorrectMovement;
import dev.ioza.bank.application.commands.CreateAccount;
import dev.ioza.bank.application.commands.CreateEnvelope;
import dev.ioza.bank.application.commands.CreateHousehold;
import dev.ioza.bank.application.commands.CreatePeriod;
import dev.ioza.bank.application.commands.Expense;
import dev.ioza.bank.application.commands.Income;
import dev.ioza.bank.application.commands.MovementReplacement;
import dev.ioza.bank.application.commands.OpeningBalance;
import dev.ioza.bank.application.commands.Reverse;
import dev.ioza.bank.application.commands.SaveBudget;
import dev.ioza.bank.application.commands.UpsertPeriodBudgetLine;
import dev.ioza.bank.application.commands.UpsertTemplateBudgetLine;
import dev.ioza.bank.application.ports.in.Budget;
import dev.ioza.bank.application.ports.in.Finance;
import dev.ioza.bank.application.ports.in.Setup;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.application.services.IdempotentMovementService;
import dev.ioza.bank.domain.enums.AccountType;
import dev.ioza.bank.domain.enums.EnvelopeType;
import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.BudgetLine;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.Funding;
import dev.ioza.bank.domain.model.Movement;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BudgetTemplateDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.MovementDoc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.TransactionException;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Uses the real Spring proxies, repositories, replica set and transaction manager. No persistence
 * mocks.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    properties = {
      "bank.auth.owner-password=integration-owner",
      "bank.auth.cecy-password=integration-editor"
    })
@Import(MongoFinanceIntegrationTest.FailureConfiguration.class)
class MongoFinanceIntegrationTest {
  @Container static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
  }

  @Autowired Setup setup;
  @Autowired Budget budget;
  @Autowired Finance finance;
  @Autowired Accounts accounts;
  @Autowired Envelopes envelopes;
  @Autowired IdempotentMovementService idempotency;
  @Autowired MongoTemplate mongo;
  @Autowired FailureListener failure;
  @Autowired TemplateFailureListener templateFailure;
  String household;
  String period;
  Account cash;
  Envelope central;
  Envelope sams;

  static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  static void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("olda", "", List.of()));
  }

  @BeforeEach
  void seed() {
    authenticate();
    failure.failNext.set(false);
    templateFailure.failNext.set(false);
    failure.failAfter.set(0);
    for (String collection : mongo.getCollectionNames()) mongo.remove(new Query(), collection);
    if (!mongo.collectionExists("envelope_creation_requests"))
      mongo.createCollection("envelope_creation_requests");
    if (!mongo.collectionExists("idempotency_records"))
      mongo.createCollection("idempotency_records");
    household = setup.createHousehold(new CreateHousehold("Familia", "Olda")).id();
    cash = setup.createAccount(household, new CreateAccount("Efectivo", AccountType.PHYSICAL));
    central =
        setup.createEnvelope(
            household, new CreateEnvelope("Central", cash.id(), EnvelopeType.SPENDING, "Comida"));
    sams =
        setup.createEnvelope(
            household, new CreateEnvelope("Sam's", cash.id(), EnvelopeType.SAVINGS, "Comida"));
    budget.saveTemplate(
        household,
        new SaveBudget(
            money("1080.00"),
            List.of(
                new BudgetLine(central.id(), money("80.00")),
                new BudgetLine(sams.id(), money("1000.00")))));
    period =
        budget
            .createPeriod(
                household,
                new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true))
            .id();
    finance.opening(household, new OpeningBalance(null, cash.id(), money("1080.00"), "Saldo real"));
    finance.allocate(household, new Allocation(period, central.id(), money("80.00"), "Central"));
    finance.allocate(household, new Allocation(period, sams.id(), money("1000.00"), "Sam's"));
  }

  Expense expense() {
    return new Expense(
        period,
        central.id(),
        money("150.00"),
        "Carne",
        List.of(new Funding(central.id(), money("80.00")), new Funding(sams.id(), money("70.00"))));
  }

  void balances(String account, String target, String donor) {
    assertEquals(0, accounts.find(cash.id()).orElseThrow().balance().compareTo(money(account)));
    assertEquals(0, envelopes.find(central.id()).orElseThrow().balance().compareTo(money(target)));
    assertEquals(0, envelopes.find(sams.id()).orElseThrow().balance().compareTo(money(donor)));
  }

  @Test
  void plansNewEnvelopeAndReplacesAmountWithoutFinancialEffects() {
    var template = budget.template(household);
    var before = budget.getPeriod(household, period);
    var movementCount = finance.movements(household).size();
    var added =
        setup.createEnvelope(
            household, new CreateEnvelope("Nuevo", cash.id(), EnvelopeType.SAVINGS, "Ahorro"));
    budget.upsertPeriodBudgetLine(
        household, period, new UpsertPeriodBudgetLine(added.id(), money("1500.00")));
    var updated =
        budget.upsertPeriodBudgetLine(
            household, period, new UpsertPeriodBudgetLine(added.id(), money("0.00")));
    assertEquals(3, updated.lines().size());
    assertEquals(before.lines(), updated.lines().subList(0, 2));
    assertEquals(new BudgetLine(added.id(), money("0.00")), updated.lines().get(2));
    assertEquals(updated, budget.getPeriod(household, period));
    assertEquals(template, budget.template(household));
    assertEquals(movementCount, finance.movements(household).size());
    balances("1080.00", "80.00", "1000.00");
  }

  @Test
  void concurrentCloseAndPlanNeverWriteIntoClosedPeriod() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var barrier = new CyclicBarrier(2);
    try {
      var edit =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier,
                      () ->
                          budget.upsertPeriodBudgetLine(
                              household,
                              period,
                              new UpsertPeriodBudgetLine(central.id(), money("200.00")))));
      var close =
          executor.submit(
              () -> attemptBudgetChange(barrier, () -> budget.close(household, period)));
      boolean edited = edit.get(30, TimeUnit.SECONDS);
      boolean closed = close.get(30, TimeUnit.SECONDS);
      assertTrue(edited || closed);
      if (!closed) budget.close(household, period);
      var result = budget.getPeriod(household, period);
      assertEquals(PeriodStatus.CLOSED, result.status());
      assertEquals(money(edited ? "200.00" : "80.00"), result.lines().get(0).amount());
      assertThrows(
          ConflictException.class,
          () ->
              budget.upsertPeriodBudgetLine(
                  household, period, new UpsertPeriodBudgetLine(central.id(), money("300.00"))));
      balances("1080.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentLineEditsPreserveBothChangesAfterRetry() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var barrier = new CyclicBarrier(2);
    var first = new UpsertPeriodBudgetLine(central.id(), money("200.00"));
    var second = new UpsertPeriodBudgetLine(sams.id(), money("300.00"));
    try {
      var a =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> budget.upsertPeriodBudgetLine(household, period, first)));
      var b =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> budget.upsertPeriodBudgetLine(household, period, second)));
      boolean savedA = a.get(30, TimeUnit.SECONDS);
      boolean savedB = b.get(30, TimeUnit.SECONDS);
      assertTrue(savedA || savedB);
      if (!savedA) budget.upsertPeriodBudgetLine(household, period, first);
      if (!savedB) budget.upsertPeriodBudgetLine(household, period, second);
      assertEquals(
          List.of(
              new BudgetLine(central.id(), money("200.00")),
              new BudgetLine(sams.id(), money("300.00"))),
          budget.getPeriod(household, period).lines());
      balances("1080.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  boolean attemptBudgetChange(CyclicBarrier barrier, Runnable action) throws Exception {
    authenticate();
    try {
      barrier.await(10, TimeUnit.SECONDS);
      action.run();
      return true;
    } catch (DataAccessException | TransactionException | ConflictException e) {
      return false;
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void smartExpensePersistsOneExpenseAndDecimal128ThroughoutAudit() {
    Movement movement = finance.expense(household, expense());
    balances("930.00", "0.00", "930.00");
    assertEquals(2, movement.funding().size());
    Document stored =
        mongo.getCollection("movements").find(new Document("_id", movement.id())).first();
    assertNotNull(stored);
    assertTrue(stored.get("amount") instanceof Decimal128);
    assertTrue(
        stored.getList("funding", Document.class).get(0).get("amount") instanceof Decimal128);
    assertTrue(
        stored.getList("accountEffects", Document.class).get(0).get("after") instanceof Decimal128);
    Document account = mongo.getCollection("accounts").find(new Document("_id", cash.id())).first();
    assertNotNull(account);
    assertTrue(account.get("balance") instanceof Decimal128);
  }

  @Test
  void previewIsReadOnlyAndConfirmationRevalidates() {
    assertEquals(
        0, finance.previewExpense(household, expense()).shortfall().compareTo(money("70.00")));
    balances("1080.00", "80.00", "1000.00");
    finance.expense(
        household, new Expense(period, sams.id(), money("1000.00"), "Otro gasto", List.of()));
    assertThrows(RuntimeException.class, () -> finance.expense(household, expense()));
    balances("80.00", "80.00", "0.00");
  }

  @Test
  void failureWritingMovementRollsBackAlreadyWrittenBalancesAndIdempotency() {
    failure.failNext.set(true);
    assertThrows(
        IllegalStateException.class,
        () ->
            idempotency.execute(
                household,
                "rollback",
                "expense",
                expense(),
                () -> finance.expense(household, expense())));
    balances("1080.00", "80.00", "1000.00");
    assertEquals(3, finance.movements(household).size());
    assertEquals(0, mongo.getCollection("idempotency_records").countDocuments());
    idempotency.execute(
        household, "rollback", "expense", expense(), () -> finance.expense(household, expense()));
    balances("930.00", "0.00", "930.00");
  }

  @Test
  void replayReturnsOriginalAndChangedBodyConflicts() {
    Movement first =
        idempotency.execute(
            household,
            "purchase",
            "expense",
            expense(),
            () -> finance.expense(household, expense()));
    Movement replay =
        idempotency.execute(
            household,
            "purchase",
            "expense",
            expense(),
            () -> {
              throw new AssertionError("No repetir");
            });
    assertEquals(first, replay);
    Expense changed = new Expense(period, central.id(), money("149.00"), "Carne", List.of());
    assertThrows(
        ConflictException.class,
        () ->
            idempotency.execute(
                household,
                "purchase",
                "expense",
                changed,
                () -> finance.expense(household, changed)));
    balances("930.00", "0.00", "930.00");
  }

  @Test
  void concurrentExpensesCannotOverspend() throws Exception {
    concurrentExpense(false);
  }

  @Test
  void concurrentSameKeyCommitsOnlyOnceAndCanBeReplayed() throws Exception {
    concurrentExpense(true);
  }

  void concurrentExpense(boolean sameKey) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a = executor.submit(() -> attempt("a", barrier));
      var b = executor.submit(() -> attempt(sameKey ? "a" : "b", barrier));
      assertEquals(1, a.get(30, TimeUnit.SECONDS) + b.get(30, TimeUnit.SECONDS));
    } finally {
      executor.shutdownNow();
    }
    balances("930.00", "0.00", "930.00");
    assertEquals(4, finance.movements(household).size());
    if (sameKey) {
      Movement replay =
          idempotency.execute(
              household,
              "a",
              "expense",
              expense(),
              () -> {
                throw new AssertionError("Duplicado");
              });
      assertEquals(0, replay.amount().compareTo(money("150.00")));
    }
  }

  int attempt(String key, CyclicBarrier barrier) {
    authenticate();
    try {
      idempotency.execute(
          household,
          key,
          "expense",
          expense(),
          () -> {
            try {
              barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            return finance.expense(household, expense());
          });
      return 1;
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException e) {
      return 0;
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void staleVersionIsRejectedByMongo() {
    Account stale = accounts.find(cash.id()).orElseThrow();
    accounts.save(stale.withBalance(money("1081.00")));
    assertThrows(
        org.springframework.dao.OptimisticLockingFailureException.class,
        () -> accounts.save(stale.withBalance(money("1082.00"))));
  }

  @Test
  void closingAndOpeningPeriodsPreservesRemaindersAndHistory() {
    budget.close(household, period);
    budget.saveTemplate(household, new SaveBudget(money("1.00"), List.of()));
    budget.createPeriod(
        household, new CreatePeriod(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 30), true));
    balances("1080.00", "80.00", "1000.00");
    assertEquals(2, budget.getPeriod(household, period).lines().size());
    assertEquals(LocalDate.of(2026, 11, 15), budget.periods(household).get(0).startDate());
    assertEquals(period, budget.periods(household).get(1).id());
    assertThrows(ConflictException.class, () -> finance.expense(household, expense()));
  }

  @Test
  void reversalAndFilteredPaginationUsePersistedEffects() {
    Movement movement = finance.expense(household, expense());
    finance.reverse(household, movement.id(), new Reverse(period, "Error"));
    balances("1080.00", "80.00", "1000.00");
    assertThrows(
        ConflictException.class,
        () -> finance.reverse(household, movement.id(), new Reverse(period, "Duplicado")));
    assertEquals(
        1,
        finance
            .movements(household, new MovementQuery(period, cash.id(), central.id(), 0, 1))
            .size());
    assertEquals(
        0,
        finance.movements(household, new MovementQuery(period, "otra-cuenta", null, 0, 10)).size());
  }

  CorrectMovement incomeCorrection(String amount) {
    return new CorrectMovement(
        period,
        "Captura",
        new MovementReplacement(
            MovementType.INCOME,
            money(amount),
            "Corregido",
            cash.id(),
            null,
            null,
            null,
            null,
            null,
            null,
            null));
  }

  @Test
  void directExpenseIsIdempotentFilterableAndReversibleWithoutReserveChanges() {
    finance.income(household, new Income(period, cash.id(), null, money("200.00"), "Libre"));
    var cmd = new AccountExpense(period, cash.id(), money("150.00"), "Directo");
    int beforeCount = finance.movements(household).size();
    finance.previewAccountExpense(household, cmd);
    assertEquals(beforeCount, finance.movements(household).size());
    var first =
        idempotency.execute(
            household,
            "direct",
            "account-expense",
            cmd,
            () -> finance.accountExpense(household, cmd));
    var replay =
        idempotency.execute(
            household,
            "direct",
            "account-expense",
            cmd,
            () -> {
              throw new AssertionError("Replay");
            });
    assertEquals(first, replay);
    balances("1130.00", "80.00", "1000.00");
    assertTrue(first.envelopeEffects().isEmpty());
    assertTrue(
        finance
            .movements(household, new MovementQuery(period, cash.id(), null, 0, 100))
            .contains(first));
    assertTrue(
        finance.movements(household, new MovementQuery(period, null, central.id(), 0, 100)).stream()
            .noneMatch(m -> m.id().equals(first.id())));
    finance.reverse(household, first.id(), new Reverse(period, "Cancelar"));
    balances("1280.00", "80.00", "1000.00");
  }

  @Test
  void spentIncomeCorrectionPersistsNetBalancesAuditAndStableReplay() {
    var original =
        finance.income(household, new Income(period, cash.id(), null, money("100.00"), "Original"));
    finance.accountExpense(
        household, new AccountExpense(period, cash.id(), money("100.00"), "Gastado"));
    var cmd = incomeCorrection("200.00");
    finance.previewCorrection(household, original.id(), cmd);
    balances("1080.00", "80.00", "1000.00");
    var replacement =
        idempotency.execute(
            household,
            "correct",
            "correct:" + original.id(),
            cmd,
            () -> finance.correct(household, original.id(), cmd));
    var replay =
        idempotency.execute(
            household,
            "correct",
            "correct:" + original.id(),
            cmd,
            () -> {
              throw new AssertionError("Replay");
            });
    assertEquals(replacement, replay);
    balances("1180.00", "80.00", "1000.00");
    assertEquals(MovementStatus.REVERSED, finance.movement(household, original.id()).status());
    assertEquals(original.occurredAt(), finance.movement(household, original.id()).occurredAt());
    assertEquals(original.id(), replacement.correction().originalId());
    assertEquals(
        original.id(),
        finance.movement(household, replacement.correction().reversalId()).reversalOf());
    var stored =
        mongo.getCollection("movements").find(new Document("_id", replacement.id())).first();
    assertNotNull(stored);
    assertTrue(stored.get("amount") instanceof Decimal128);
    assertTrue(
        stored.getList("accountEffects", Document.class).get(0).get("change")
            instanceof Decimal128);
    assertEquals(original.id(), stored.get("correction", Document.class).getString("originalId"));
    assertThrows(ConflictException.class, () -> finance.correct(household, original.id(), cmd));
  }

  @Test
  void failedReplacementWriteRollsBackCompensationOriginalBalancesAndKey() {
    var original =
        finance.income(household, new Income(period, cash.id(), null, money("100.00"), "Original"));
    int count = finance.movements(household).size();
    var cmd = incomeCorrection("200.00");
    failure.failAfter.set(3); // compensation, original status, then replacement fails
    assertThrows(
        IllegalStateException.class,
        () ->
            idempotency.execute(
                household,
                "failed-correction",
                "correct:" + original.id(),
                cmd,
                () -> finance.correct(household, original.id(), cmd)));
    balances("1180.00", "80.00", "1000.00");
    assertEquals(original, finance.movement(household, original.id()));
    assertEquals(count, finance.movements(household).size());
    assertEquals(0, mongo.getCollection("idempotency_records").countDocuments());
    idempotency.execute(
        household,
        "failed-correction",
        "correct:" + original.id(),
        cmd,
        () -> finance.correct(household, original.id(), cmd));
    balances("1280.00", "80.00", "1000.00");
  }

  @Test
  void invalidNetCorrectionRollsBackWithoutHistoryOrKey() {
    var original =
        finance.income(household, new Income(period, cash.id(), null, money("100.00"), "Original"));
    finance.accountExpense(
        household, new AccountExpense(period, cash.id(), money("100.00"), "Gastado"));
    int count = finance.movements(household).size();
    var cmd = incomeCorrection("50.00");
    assertThrows(
        ValidationException.class,
        () ->
            idempotency.execute(
                household,
                "invalid-correction",
                "correct:" + original.id(),
                cmd,
                () -> finance.correct(household, original.id(), cmd)));
    balances("1080.00", "80.00", "1000.00");
    assertEquals(original, finance.movement(household, original.id()));
    assertEquals(count, finance.movements(household).size());
    assertEquals(0, mongo.getCollection("idempotency_records").countDocuments());
  }

  @Test
  void concurrentCorrectionAndReversalCompensateOriginalOnlyOnce() throws Exception {
    var original =
        finance.income(household, new Income(period, cash.id(), null, money("100.00"), "Original"));
    var cmd = incomeCorrection("200.00");
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () ->
                  attemptFinancial(
                      barrier, "correction", () -> finance.correct(household, original.id(), cmd)));
      var b =
          executor.submit(
              () ->
                  attemptFinancial(
                      barrier,
                      "reversal",
                      () ->
                          finance.reverse(
                              household, original.id(), new Reverse(period, "Cancelar"))));
      int corrected = a.get(30, TimeUnit.SECONDS), reversed = b.get(30, TimeUnit.SECONDS);
      assertEquals(1, corrected + reversed);
      balances(corrected == 1 ? "1280.00" : "1080.00", "80.00", "1000.00");
      assertEquals(
          1,
          finance.movements(household).stream()
              .filter(m -> original.id().equals(m.reversalOf()))
              .count());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentDirectExpensesCannotSpendTheSameFreeFunds() throws Exception {
    finance.income(household, new Income(period, cash.id(), null, money("100.00"), "Libre"));
    var cmd = new AccountExpense(period, cash.id(), money("80.00"), "Directo");
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () ->
                  attemptFinancial(
                      barrier, "direct-a", () -> finance.accountExpense(household, cmd)));
      var b =
          executor.submit(
              () ->
                  attemptFinancial(
                      barrier, "direct-b", () -> finance.accountExpense(household, cmd)));
      assertEquals(1, a.get(30, TimeUnit.SECONDS) + b.get(30, TimeUnit.SECONDS));
      balances("1100.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  int attemptFinancial(CyclicBarrier barrier, String key, Supplier<Movement> action) {
    authenticate();
    try {
      idempotency.execute(
          household,
          key,
          key,
          key,
          () -> {
            try {
              barrier.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
            return action.get();
          });
      return 1;
    } catch (DataAccessException
        | TransactionException
        | ConflictException
        | ValidationException e) {
      return 0;
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void firstHabitualEnvelopeCreatesTemplateReplaysAndCopiesIntoPeriod() {
    String h = setup.createHousehold(new CreateHousehold("Inicial", "Olda")).id();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var cmd =
        new CreateEnvelope("Primero", a.id(), EnvelopeType.SAVINGS, "Ahorro", money("4500.00"));
    var created = setup.createEnvelopeWithPlan(h, cmd, "habitual-first");
    assertEquals(created, setup.createEnvelopeWithPlan(h, cmd, "habitual-first"));
    assertEquals(1, setup.envelopes(h).size());
    assertEquals(money("0.00"), created.envelope().balance());
    assertEquals(money("0.00"), accounts.find(a.id()).orElseThrow().balance());
    var template = budget.template(h);
    assertEquals(money("0.00"), template.expectedIncome());
    assertEquals(
        List.of(new BudgetLine(created.envelope().id(), money("4500.00"))), template.lines());
    var p =
        budget.createPeriod(
            h, new CreatePeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 14), true));
    assertEquals(template.lines(), p.lines());
    budget.upsertTemplateBudgetLine(
        h, new UpsertTemplateBudgetLine(created.envelope().id(), money("4800.00")));
    assertEquals(p, budget.getPeriod(h, p.id()));
    assertEquals(money("4800.00"), setup.envelopesWithPlans(h).get(0).habitualAmount());
    assertEquals(created, setup.createEnvelopeWithPlan(h, cmd, "habitual-first"));
    assertEquals(0, finance.movements(h).size());
    Document stored =
        mongo.getCollection("budget_templates").find(new Document("householdId", h)).first();
    assertNotNull(stored);
    assertTrue(stored.getList("lines", Document.class).get(0).get("amount") instanceof Decimal128);
    assertThrows(
        ConflictException.class,
        () ->
            setup.createEnvelopeWithPlan(
                h,
                new CreateEnvelope(
                    "Distinto", a.id(), EnvelopeType.SAVINGS, "Ahorro", money("4500.00")),
                "habitual-first"));
  }

  @Test
  void templateWriteFailureRollsBackEnvelopePlanAndCreationReplayRecord() {
    String h = setup.createHousehold(new CreateHousehold("Inicial", "Olda")).id();
    var a = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var cmd = new CreateEnvelope("Nuevo", a.id(), EnvelopeType.SAVINGS, "Ahorro", money("750.00"));
    long receipts = mongo.getCollection("envelope_creation_requests").countDocuments();
    templateFailure.failNext.set(true);
    assertThrows(
        IllegalStateException.class,
        () -> setup.createEnvelopeWithPlan(h, cmd, "rollback-envelope"));
    assertTrue(setup.envelopes(h).isEmpty());
    assertThrows(NotFoundException.class, () -> budget.template(h));
    assertEquals(receipts, mongo.getCollection("envelope_creation_requests").countDocuments());
    setup.createEnvelopeWithPlan(h, cmd, "rollback-envelope");
    assertEquals(1, setup.envelopes(h).size());
    assertEquals(1, budget.template(h).lines().size());
  }

  @Test
  void concurrentHabitualLineUpdatesPreserveOtherLinesAndExpectedIncome() throws Exception {
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var first = new UpsertTemplateBudgetLine(central.id(), money("200.00"));
      var second = new UpsertTemplateBudgetLine(sams.id(), money("300.00"));
      var a =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> budget.upsertTemplateBudgetLine(household, first)));
      var b =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> budget.upsertTemplateBudgetLine(household, second)));
      boolean savedA = a.get(30, TimeUnit.SECONDS), savedB = b.get(30, TimeUnit.SECONDS);
      assertTrue(savedA || savedB);
      if (!savedA) budget.upsertTemplateBudgetLine(household, first);
      if (!savedB) budget.upsertTemplateBudgetLine(household, second);
      var template = budget.template(household);
      assertEquals(
          List.of(
              new BudgetLine(central.id(), money("200.00")),
              new BudgetLine(sams.id(), money("300.00"))),
          template.lines());
      assertEquals(money("1080.00"), template.expectedIncome());
      balances("1080.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentEnvelopeCreationWithSameKeyProducesOneEnvelopeAndOneLine() throws Exception {
    var cmd =
        new CreateEnvelope("Nuevo", cash.id(), EnvelopeType.SAVINGS, "Ahorro", money("750.00"));
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> setup.createEnvelopeWithPlan(household, cmd, "same-create")));
      var b =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> setup.createEnvelopeWithPlan(household, cmd, "same-create")));
      boolean savedA = a.get(30, TimeUnit.SECONDS), savedB = b.get(30, TimeUnit.SECONDS);
      assertTrue(savedA || savedB);
      var replay = setup.createEnvelopeWithPlan(household, cmd, "same-create");
      assertEquals(3, setup.envelopes(household).size());
      assertEquals(3, budget.template(household).lines().size());
      assertEquals(
          1,
          budget.template(household).lines().stream()
              .filter(l -> l.envelopeId().equals(replay.envelope().id()))
              .count());
      assertEquals(1, mongo.getCollection("envelope_creation_requests").countDocuments());
      balances("1080.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentFirstPlansCreateOneTemplateAndPreserveBothEnvelopesAfterRetry() throws Exception {
    String h = setup.createHousehold(new CreateHousehold("Inicial", "Olda")).id();
    var account = setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var first =
        new CreateEnvelope("Uno", account.id(), EnvelopeType.SAVINGS, "Ahorro", money("100.00"));
    var second =
        new CreateEnvelope("Dos", account.id(), EnvelopeType.SAVINGS, "Ahorro", money("200.00"));
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> setup.createEnvelopeWithPlan(h, first, "first")));
      var b =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> setup.createEnvelopeWithPlan(h, second, "second")));
      boolean savedA = a.get(30, TimeUnit.SECONDS), savedB = b.get(30, TimeUnit.SECONDS);
      assertTrue(savedA || savedB);
      var createdA = setup.createEnvelopeWithPlan(h, first, "first");
      var createdB = setup.createEnvelopeWithPlan(h, second, "second");
      assertEquals(2, setup.envelopes(h).size());
      assertEquals(2, budget.template(h).lines().size());
      assertTrue(
          budget
              .template(h)
              .lines()
              .contains(new BudgetLine(createdA.envelope().id(), money("100.00"))));
      assertTrue(
          budget
              .template(h)
              .lines()
              .contains(new BudgetLine(createdB.envelope().id(), money("200.00"))));
      assertEquals(
          1,
          mongo.getCollection("budget_templates").countDocuments(new Document("householdId", h)));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void concurrentPeriodCreationSeesCompleteHabitualSnapshotBeforeOrAfterEnvelopeCreation()
      throws Exception {
    var before = budget.template(household);
    var cmd =
        new CreateEnvelope("Nuevo", cash.id(), EnvelopeType.SAVINGS, "Ahorro", money("750.00"));
    var periodCmd = new CreatePeriod(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 30), true);
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> setup.createEnvelopeWithPlan(household, cmd, "period-race")));
      var b =
          executor.submit(
              () -> attemptBudgetChange(barrier, () -> budget.createPeriod(household, periodCmd)));
      boolean savedA = a.get(30, TimeUnit.SECONDS), savedB = b.get(30, TimeUnit.SECONDS);
      assertTrue(savedA || savedB);
      setup.createEnvelopeWithPlan(household, cmd, "period-race");
      if (!savedB) budget.createPeriod(household, periodCmd);
      var snapshot =
          budget.periods(household).stream()
              .filter(p -> p.startDate().equals(periodCmd.startDate()))
              .findFirst()
              .orElseThrow();
      var after = budget.template(household);
      assertTrue(snapshot.lines().equals(before.lines()) || snapshot.lines().equals(after.lines()));
      assertEquals(3, after.lines().size());
      assertEquals(before.expectedIncome(), snapshot.expectedIncome());
      balances("1080.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void fullTemplateReplacementAndLineUpdateSerializeAsCompleteOperations() throws Exception {
    var replacement =
        new SaveBudget(
            money("5000.00"),
            List.of(
                new BudgetLine(central.id(), money("90.00")),
                new BudgetLine(sams.id(), money("1000.00"))));
    var line = new UpsertTemplateBudgetLine(central.id(), money("200.00"));
    var barrier = new CyclicBarrier(2);
    var executor = Executors.newFixedThreadPool(2);
    try {
      var a =
          executor.submit(
              () ->
                  attemptBudgetChange(barrier, () -> budget.saveTemplate(household, replacement)));
      var b =
          executor.submit(
              () ->
                  attemptBudgetChange(
                      barrier, () -> budget.upsertTemplateBudgetLine(household, line)));
      boolean savedA = a.get(30, TimeUnit.SECONDS), savedB = b.get(30, TimeUnit.SECONDS);
      assertTrue(savedA || savedB);
      if (!savedA) budget.saveTemplate(household, replacement);
      if (!savedB) budget.upsertTemplateBudgetLine(household, line);
      var result = budget.template(household);
      assertEquals(money("5000.00"), result.expectedIncome());
      assertEquals(2, result.lines().size());
      assertEquals(new BudgetLine(sams.id(), money("1000.00")), result.lines().get(1));
      assertTrue(List.of(money("90.00"), money("200.00")).contains(result.lines().get(0).amount()));
      balances("1080.00", "80.00", "1000.00");
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void zeroMultiFundingExpensePersistsRefundResourcesAndReplayThenAllowsSecondCorrection() {
    var original = finance.expense(household, expense());
    var cmd =
        new CorrectMovement(
            period,
            "Cero",
            new MovementReplacement(
                MovementType.EXPENSE,
                money("0.00"),
                "Corregido",
                null,
                null,
                central.id(),
                null,
                null,
                null,
                null,
                List.of()));
    var zero =
        idempotency.execute(
            household,
            "zero-expense",
            "correct:" + original.id(),
            cmd,
            () -> finance.correct(household, original.id(), cmd));
    balances("1080.00", "80.00", "1000.00");
    assertEquals(
        zero,
        idempotency.execute(
            household,
            "zero-expense",
            "correct:" + original.id(),
            cmd,
            () -> {
              throw new AssertionError("Replay");
            }));
    assertTrue(zero.funding().isEmpty());
    assertTrue(zero.accountEffects().isEmpty());
    assertTrue(zero.envelopeEffects().isEmpty());
    assertEquals(
        central.id(),
        MovementResponse.from(finance.movement(household, zero.id()))
            .replacement()
            .targetEnvelopeId());
    assertEquals(MovementStatus.REVERSED, finance.movement(household, original.id()).status());
    var stored = mongo.getCollection("movements").find(new Document("_id", zero.id())).first();
    assertNotNull(stored);
    assertTrue(stored.get("amount") instanceof Decimal128);
    assertEquals(
        central.id(),
        stored
            .get("correction", Document.class)
            .get("replacement", Document.class)
            .getString("envelopeId"));
    var next =
        new CorrectMovement(
            period,
            "Segunda",
            new MovementReplacement(
                MovementType.EXPENSE,
                money("20.00"),
                "Nuevo",
                null,
                null,
                central.id(),
                null,
                null,
                null,
                null,
                null));
    finance.correct(household, zero.id(), next);
    balances("1060.00", "60.00", "1000.00");
  }

  @Test
  void zeroAccountExpensePreservesReservesAndExplicitAccount() {
    finance.income(household, new Income(period, cash.id(), null, money("100.00"), "Libre"));
    var original =
        finance.accountExpense(
            household, new AccountExpense(period, cash.id(), money("100.00"), "Original"));
    var cmd =
        new CorrectMovement(
            period,
            "Cero",
            new MovementReplacement(
                MovementType.EXPENSE,
                money("0.00"),
                "Corregido",
                cash.id(),
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    var zero = finance.correct(household, original.id(), cmd);
    balances("1180.00", "80.00", "1000.00");
    assertEquals(
        "ACCOUNT_EXPENSE",
        MovementResponse.from(finance.movement(household, zero.id())).replacement().type());
    assertEquals(
        cash.id(),
        MovementResponse.from(finance.movement(household, zero.id())).replacement().accountId());
    assertTrue(zero.envelopeEffects().isEmpty());
  }

  @Test
  void zeroReplacementFailureRollsBackRefundOriginalHistoryAndIdempotency() {
    var original = finance.expense(household, expense());
    var cmd =
        new CorrectMovement(
            period,
            "Cero",
            new MovementReplacement(
                MovementType.EXPENSE,
                money("0.00"),
                "Corregido",
                null,
                null,
                central.id(),
                null,
                null,
                null,
                null,
                List.of()));
    int count = finance.movements(household).size();
    failure.failAfter.set(3);
    assertThrows(
        IllegalStateException.class,
        () ->
            idempotency.execute(
                household,
                "zero-rollback",
                "correct:" + original.id(),
                cmd,
                () -> finance.correct(household, original.id(), cmd)));
    balances("930.00", "0.00", "930.00");
    assertEquals(original, finance.movement(household, original.id()));
    assertEquals(count, finance.movements(household).size());
    assertEquals(0, mongo.getCollection("idempotency_records").countDocuments());
    idempotency.execute(
        household,
        "zero-rollback",
        "correct:" + original.id(),
        cmd,
        () -> finance.correct(household, original.id(), cmd));
    balances("1080.00", "80.00", "1000.00");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FailureConfiguration {
    @Bean
    TemplateFailureListener templateFailureListener() {
      return new TemplateFailureListener();
    }

    @Bean
    FailureListener failureListener() {
      return new FailureListener();
    }
  }

  /**
   * Technical test fixture: injects a failure after account/envelope saves, before history is
   * saved.
   */
  static class FailureListener extends AbstractMongoEventListener<MovementDoc> {
    final AtomicBoolean failNext = new AtomicBoolean();
    final AtomicInteger failAfter = new AtomicInteger();

    @Override
    public void onBeforeConvert(BeforeConvertEvent<MovementDoc> event) {
      if ((failAfter.get() > 0 && failAfter.decrementAndGet() == 0)
          || failNext.compareAndSet(true, false))
        throw new IllegalStateException("Fallo de historial inyectado");
    }
  }

  static class TemplateFailureListener extends AbstractMongoEventListener<BudgetTemplateDoc> {
    final AtomicBoolean failNext = new AtomicBoolean();

    @Override
    public void onBeforeConvert(BeforeConvertEvent<BudgetTemplateDoc> event) {
      if (failNext.compareAndSet(true, false))
        throw new IllegalStateException("Fallo de plantilla inyectado");
    }
  }
}

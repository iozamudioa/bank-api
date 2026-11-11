package dev.ioza.bank.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ioza.bank.application.commands.AccountExpense;
import dev.ioza.bank.application.commands.Allocation;
import dev.ioza.bank.application.commands.CorrectMovement;
import dev.ioza.bank.application.commands.EnvelopeTransfer;
import dev.ioza.bank.application.commands.Expense;
import dev.ioza.bank.application.commands.Income;
import dev.ioza.bank.application.commands.MovementReplacement;
import dev.ioza.bank.application.commands.OpeningBalance;
import dev.ioza.bank.application.commands.Reallocation;
import dev.ioza.bank.application.commands.Reverse;
import dev.ioza.bank.application.commands.Transfer;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Budgets;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.application.ports.out.Households;
import dev.ioza.bank.application.ports.out.Incomes;
import dev.ioza.bank.application.ports.out.Members;
import dev.ioza.bank.application.ports.out.Movements;
import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.domain.Money;
import dev.ioza.bank.domain.enums.AccountType;
import dev.ioza.bank.domain.enums.EnvelopeType;
import dev.ioza.bank.domain.enums.IncomeSourceType;
import dev.ioza.bank.domain.enums.MemberRole;
import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.ForbiddenException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.Funding;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.domain.model.IncomeSource;
import dev.ioza.bank.domain.model.Movement;
import dev.ioza.bank.infrastructure.adapters.in.controllers.MovementController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.advice.ApiExceptionHandler;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.MovementResponse;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Pure application service tests; MongoDB transaction integration tests belong in the next stage.
 */
class MovementServiceTest {
  private static BigDecimal money(String x) {
    return new BigDecimal(x);
  }

  private final Map<String, Account> accountData = new HashMap<>();
  private final Map<String, IncomeSource> incomeData = new HashMap<>();
  private final Map<String, Envelope> envelopeData = new HashMap<>();
  private final Map<String, Movement> movementData = new HashMap<>();
  private final Map<String, BudgetPeriod> periodData = new HashMap<>();
  private MovementService service;
  private MemberRole testRole = MemberRole.OWNER;
  private boolean testActive = true;
  private final Accounts accounts =
      new Accounts() {
        public Account save(Account a) {
          accountData.put(a.id(), a);
          return a;
        }

        public Optional<Account> find(String id) {
          return Optional.ofNullable(accountData.get(id));
        }

        public List<Account> all(String h) {
          return accountData.values().stream().filter(a -> a.householdId().equals(h)).toList();
        }
      };
  private final Envelopes envelopes =
      new Envelopes() {
        public Envelope save(Envelope e) {
          envelopeData.put(e.id(), e);
          return e;
        }

        public Optional<Envelope> find(String id) {
          return Optional.ofNullable(envelopeData.get(id));
        }

        public List<Envelope> all(String h) {
          return envelopeData.values().stream().filter(e -> e.householdId().equals(h)).toList();
        }

        public List<Envelope> byAccount(String h, String a) {
          return all(h).stream().filter(e -> e.accountId().equals(a)).toList();
        }
      };
  private final Incomes incomes =
      new Incomes() {
        public IncomeSource save(IncomeSource s) {
          incomeData.put(s.id(), s);
          return s;
        }

        public Optional<IncomeSource> find(String id) {
          return Optional.ofNullable(incomeData.get(id));
        }

        public List<IncomeSource> all(String h) {
          return List.of();
        }
      };
  private final Budgets budgets =
      new Budgets() {
        public void lockPeriod(String id) {}

        public void lockSchedule(String id) {}

        public BudgetTemplate saveTemplate(BudgetTemplate t) {
          return t;
        }

        public Optional<BudgetTemplate> getTemplate(String h) {
          return Optional.empty();
        }

        public BudgetPeriod savePeriod(BudgetPeriod p) {
          periodData.put(p.id(), p);
          return p;
        }

        public Optional<BudgetPeriod> getPeriod(String id) {
          return Optional.ofNullable(periodData.get(id));
        }

        public Optional<BudgetPeriod> findStart(String h, LocalDate d) {
          return Optional.empty();
        }

        public List<BudgetPeriod> periods(String h) {
          return List.of();
        }
      };
  private final Movements movements =
      new Movements() {
        public List<Movement> search(String h, MovementQuery q) {
          return recent(h);
        }

        public boolean hasOpening(String h, String a) {
          return movementData.values().stream()
              .anyMatch(
                  m ->
                      m.householdId().equals(h)
                          && m.type() == MovementType.OPENING_BALANCE
                          && m.accountEffects().stream().anyMatch(e -> e.entityId().equals(a)));
        }

        public Movement save(Movement m) {
          movementData.put(m.id(), m);
          return m;
        }

        public Optional<Movement> find(String id) {
          return Optional.ofNullable(movementData.get(id));
        }

        public List<Movement> recent(String h) {
          return movementData.values().stream().toList();
        }

        public boolean hasReversal(String id) {
          return movementData.values().stream().anyMatch(m -> id.equals(m.reversalOf()));
        }
      };

  @BeforeEach
  void setup() {
    testRole = MemberRole.OWNER;
    testActive = true;
    accountData.clear();
    incomeData.clear();
    envelopeData.clear();
    movementData.clear();
    periodData.clear();
    accountData.put(
        "cash",
        new Account("cash", "h", "Efectivo", AccountType.PHYSICAL, money("1080.00"), 0L, true));
    envelopeData.put(
        "central",
        new Envelope(
            "central",
            "h",
            "Central",
            "cash",
            EnvelopeType.SPENDING,
            "Alimentación",
            money("80.00"),
            0L,
            true));
    envelopeData.put(
        "sams",
        new Envelope(
            "sams",
            "h",
            "Sam's",
            "cash",
            EnvelopeType.SAVINGS,
            "Alimentación",
            money("1000.00"),
            0L,
            true));
    periodData.put(
        "q1",
        new BudgetPeriod(
            "q1",
            "h",
            LocalDate.parse("2026-11-15"),
            LocalDate.parse("2026-11-30"),
            PeriodStatus.OPEN,
            money("21000.00"),
            List.of()));
    Households households =
        new Households() {
          public Household save(Household h) {
            return h;
          }

          public Optional<Household> find(String id) {
            return Optional.of(new Household("h", "Familia", "MXN", "America/Mexico_City", true));
          }

          public List<Household> all() {
            return List.of();
          }
        };
    Members members =
        new Members() {
          public HouseholdMember save(HouseholdMember m) {
            return m;
          }

          public Optional<HouseholdMember> find(String h, String u) {
            return Optional.of(new HouseholdMember("m", "h", "olda", "Olda", testRole, testActive));
          }

          public List<HouseholdMember> all(String h) {
            return List.of();
          }
        };
    var access = new AccessService(households, members, () -> "olda");
    service = new MovementService(accounts, envelopes, incomes, budgets, movements, access);
  }

  @Test
  void gastoInteligenteConDosSobresYConservacion() {
    Movement movement =
        service.expense(
            "h",
            new Expense(
                "q1",
                "central",
                money("150.00"),
                "Carne",
                List.of(
                    new Funding("central", money("80.00")), new Funding("sams", money("70.00")))));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("930.00")));
    assertEquals(0, envelopeData.get("central").balance().compareTo(money("0.00")));
    assertEquals(0, envelopeData.get("sams").balance().compareTo(money("930.00")));
    assertEquals(0, movement.amount().compareTo(money("150.00")));
    assertEquals(2, movement.funding().size());
    assertEquals(0, movement.occurredAt().getNano() % 1_000_000);
    assertEquals(
        0,
        accountData
            .get("cash")
            .balance()
            .subtract(
                envelopeData.values().stream()
                    .map(Envelope::balance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
            .signum());
  }

  @Test
  void reasignarEntreSobresNoAlteraElDineroFisico() {
    service.reallocate("h", new Reallocation("q1", "sams", "central", money("70.00"), "Apoyo gas"));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1080.00")));
    assertEquals(0, envelopeData.get("central").balance().compareTo(money("150.00")));
    assertEquals(0, envelopeData.get("sams").balance().compareTo(money("930.00")));
  }

  @Test
  void montoInsuficienteNoPermiteGasto() {
    assertThrows(
        ValidationException.class,
        () ->
            service.expense(
                "h",
                new Expense(
                    "q1",
                    "central",
                    money("150.00"),
                    "Carne",
                    List.of(
                        new Funding("central", money("80.00")),
                        new Funding("sams", money("65.00"))))));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1080.00")));
  }

  @Test
  void previewMuestraDonantesSuficientes() {
    var preview =
        service.previewExpense(
            "h", new Expense("q1", "central", money("150.00"), "Carne", List.of()));
    assertEquals(0, preview.shortfall().compareTo(money("70.00")));
    assertEquals(1, preview.eligibleDonors().size());
    assertEquals("sams", preview.eligibleDonors().get(0).envelopeId());
  }

  @Test
  void aporteYLiberacionNoMuevenSaldoDeCuenta() {
    accountData.put(
        "digital",
        new Account("digital", "h", "Banco", AccountType.DIGITAL, money("1000.00"), 0L, true));
    envelopeData.put(
        "emergencias",
        new Envelope(
            "emergencias",
            "h",
            "Emergencias",
            "digital",
            EnvelopeType.SAVINGS,
            "Ahorro",
            money("0.00"),
            0L,
            true));
    service.allocate("h", new Allocation("q1", "emergencias", money("669.00"), "Apartado"));
    assertEquals(0, accountData.get("digital").balance().compareTo(money("1000.00")));
    assertEquals(0, envelopeData.get("emergencias").balance().compareTo(money("669.00")));
    service.release("h", new Allocation("q1", "emergencias", money("169.00"), "Liberado"));
    assertEquals(0, envelopeData.get("emergencias").balance().compareTo(money("500.00")));
  }

  @Test
  void transferenciaDeDineroLibreConservaPatrimonio() {
    accountData.put(
        "digital",
        new Account("digital", "h", "Banco", AccountType.DIGITAL, money("1050.00"), 0L, true));
    service.transfer("h", new Transfer("q1", "digital", "cash", money("500.00"), "Retiro"));
    assertEquals(0, accountData.get("digital").balance().compareTo(money("550.00")));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1580.00")));
  }

  @Test
  void transferenciaEntreSobresDeDiferentesCuentasMueveAmbosSaldos() {
    accountData.put(
        "digital",
        new Account("digital", "h", "Banco", AccountType.DIGITAL, money("0.00"), 0L, true));
    envelopeData.put(
        "emergencias",
        new Envelope(
            "emergencias",
            "h",
            "Emergencias",
            "digital",
            EnvelopeType.SAVINGS,
            "Ahorro",
            money("0.00"),
            0L,
            true));
    service.transferEnvelope(
        "h",
        new EnvelopeTransfer("q1", "central", "emergencias", money("50.00"), "Pasar a digital"));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1030.00")));
    assertEquals(0, accountData.get("digital").balance().compareTo(money("50.00")));
    assertEquals(0, envelopeData.get("central").balance().compareTo(money("30.00")));
    assertEquals(0, envelopeData.get("emergencias").balance().compareTo(money("50.00")));
  }

  @Test
  void revertirGastoRestauraBalancesSiNoHayGastosPosteriores() {
    var gasto =
        service.expense(
            "h",
            new Expense(
                "q1",
                "central",
                money("150.00"),
                "Carne",
                List.of(
                    new Funding("central", money("80.00")), new Funding("sams", money("70.00")))));
    service.reverse("h", gasto.id(), new Reverse("q1", "Captura equivocada"));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1080.00")));
    assertEquals(0, envelopeData.get("central").balance().compareTo(money("80.00")));
    assertEquals(0, envelopeData.get("sams").balance().compareTo(money("1000.00")));
    assertEquals(MovementStatus.REVERSED, movementData.get(gasto.id()).status());
  }

  @Test
  void ingresoIncrementaSoloCuentaYSaldoInicialNoSePuedeRepetir() {
    accountData.put(
        "digital",
        new Account("digital", "h", "Banco", AccountType.DIGITAL, money("0.00"), 0L, true));
    var opening =
        service.opening("h", new OpeningBalance(null, "digital", money("100.00"), "Inicial"));
    service.reverse("h", opening.id(), new Reverse("q1", "Error"));
    assertThrows(
        ConflictException.class,
        () ->
            service.opening(
                "h", new OpeningBalance(null, "digital", money("100.00"), "Duplicado")));
    service.income("h", new Income("q1", "digital", null, money("50.00"), "Ingreso real"));
    assertEquals(0, accountData.get("digital").balance().compareTo(money("50.00")));
    assertEquals(0, envelopeData.get("central").balance().compareTo(money("80.00")));
  }

  @Test
  void gastoSimpleSoloDescuentaUnaVez() {
    service.expense("h", new Expense("q1", "central", money("50.00"), "Compra", List.of()));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1030.00")));
    assertEquals(0, envelopeData.get("central").balance().compareTo(money("30.00")));
    assertEquals(1, movementData.size());
  }

  @Test
  void rechazaGastoMayorAlSaldoSinGuardarNada() {
    assertThrows(
        ValidationException.class,
        () ->
            service.expense(
                "h", new Expense("q1", "central", money("81.00"), "Compra", List.of())));
    assertEquals(0, accountData.get("cash").balance().compareTo(money("1080.00")));
    assertEquals(0, movementData.size());
  }

  @Test
  void previewIncluyeVariosDonantesPequenosSinEscribir() {
    envelopeData.put("sams", envelopeData.get("sams").withBalance(money("40.00")));
    envelopeData.put(
        "otro",
        new Envelope(
            "otro",
            "h",
            "Otro",
            "cash",
            EnvelopeType.SPENDING,
            "Comida",
            money("30.00"),
            0L,
            true));
    var preview =
        service.previewExpense(
            "h", new Expense("q1", "central", money("150.00"), "Compra", List.of()));
    assertEquals(2, preview.eligibleDonors().size());
    assertEquals(0, movementData.size());
  }

  @Test
  void rechazaFinanciamientoDesdeOtraCuentaODuplicado() {
    accountData.put(
        "digital",
        new Account("digital", "h", "Banco", AccountType.DIGITAL, money("100.00"), 0L, true));
    envelopeData.put(
        "otro",
        new Envelope(
            "otro",
            "h",
            "Otro",
            "digital",
            EnvelopeType.SPENDING,
            "Comida",
            money("100.00"),
            0L,
            true));
    assertThrows(
        ValidationException.class,
        () ->
            service.expense(
                "h",
                new Expense(
                    "q1",
                    "central",
                    money("150.00"),
                    "Compra",
                    List.of(
                        new Funding("central", money("80.00")),
                        new Funding("otro", money("70.00"))))));
    assertThrows(
        ValidationException.class,
        () ->
            service.expense(
                "h",
                new Expense(
                    "q1",
                    "central",
                    money("50.00"),
                    "Compra",
                    List.of(
                        new Funding("central", money("25.00")),
                        new Funding("central", money("25.00"))))));
    assertEquals(0, movementData.size());
  }

  @Test
  void noPermiteTransferirReservasComoDineroLibre() {
    accountData.put(
        "digital",
        new Account("digital", "h", "Banco", AccountType.DIGITAL, money("0.00"), 0L, true));
    assertThrows(
        ConflictException.class,
        () ->
            service.transfer(
                "h", new Transfer("q1", "cash", "digital", money("1.00"), "Inválido")));
    assertEquals(0, movementData.size());
  }

  @Test
  void reversionSeValidaContraSaldosActuales() {
    service.release("h", new Allocation("q1", "sams", money("1000.00"), "Liberar"));
    var allocated =
        service.allocate("h", new Allocation("q1", "central", money("100.00"), "Apartar"));
    service.expense("h", new Expense("q1", "central", money("180.00"), "Consumir", List.of()));
    assertThrows(
        ValidationException.class,
        () -> service.reverse("h", allocated.id(), new Reverse("q1", "Demasiado tarde")));
    assertEquals(0, envelopeData.get("central").balance().signum());
    assertEquals(MovementStatus.POSTED, movementData.get(allocated.id()).status());
  }

  private MovementReplacement replacement(
      MovementType type,
      String amount,
      String accountId,
      String envelopeId,
      String fromAccountId,
      String toAccountId,
      String fromEnvelopeId,
      String toEnvelopeId,
      List<Funding> funding) {
    return new MovementReplacement(
        type,
        money(amount),
        "Corregido",
        accountId,
        null,
        envelopeId,
        fromAccountId,
        toAccountId,
        fromEnvelopeId,
        toEnvelopeId,
        funding);
  }

  private CorrectMovement correction(MovementReplacement replacement) {
    return new CorrectMovement("q1", "Error de captura", replacement);
  }

  @Test
  void directAccountExpenseUsesOnlyFreeMoneyAndCanBeReversed() {
    service.income("h", new Income("q1", "cash", null, money("200.00"), "Ingreso"));
    var envelopesBefore = Map.copyOf(envelopeData);
    int count = movementData.size();
    var c = new AccountExpense("q1", "cash", money("150.00"), "Gasto directo");
    var preview = service.previewAccountExpense("h", c);
    assertEquals(money("1280.00"), preview.totalBefore());
    assertEquals(money("200.00"), preview.freeBefore());
    assertEquals(money("1130.00"), preview.totalAfter());
    assertEquals(money("50.00"), preview.freeAfter());
    assertEquals(count, movementData.size());
    assertEquals(money("1280.00"), accountData.get("cash").balance());
    var result = service.accountExpense("h", c);
    assertEquals(MovementType.EXPENSE, result.type());
    assertNull(result.targetEnvelopeId());
    assertTrue(result.funding().isEmpty());
    assertTrue(result.envelopeEffects().isEmpty());
    assertEquals(money("-150.00"), result.accountEffects().get(0).change());
    assertEquals(envelopesBefore, envelopeData);
    service.reverse("h", result.id(), new Reverse("q1", "Cancelar"));
    assertEquals(money("1280.00"), accountData.get("cash").balance());
    assertEquals(envelopesBefore, envelopeData);
  }

  @Test
  void directAccountExpenseRejectsReservedFundsAndInvalidMoneyWithoutWrites() {
    var before = Map.copyOf(accountData);
    for (String value : List.of("1.00", "-1.00", "0.00", "1.001", "1E33")) {
      var c = new AccountExpense("q1", "cash", money(value), "Gasto");
      assertThrows(ValidationException.class, () -> service.accountExpense("h", c));
      assertThrows(ValidationException.class, () -> service.previewAccountExpense("h", c));
    }
    assertEquals(before, accountData);
    assertTrue(movementData.isEmpty());
    assertThrows(
        NotFoundException.class,
        () ->
            service.accountExpense(
                "h", new AccountExpense("q1", "missing", money("1.00"), "Gasto")));
    accountData.put(
        "other",
        new Account("other", "other", "Ajena", AccountType.DIGITAL, money("100.00"), 0L, true));
    assertThrows(
        NotFoundException.class,
        () ->
            service.accountExpense("h", new AccountExpense("q1", "other", money("1.00"), "Gasto")));
    accountData.put(
        "inactive",
        new Account("inactive", "h", "Inactiva", AccountType.DIGITAL, money("100.00"), 0L, false));
    assertThrows(
        NotFoundException.class,
        () ->
            service.accountExpense(
                "h", new AccountExpense("q1", "inactive", money("1.00"), "Gasto")));
    var p = periodData.get("q1");
    periodData.put(
        "q1",
        new BudgetPeriod(
            p.id(),
            p.householdId(),
            p.startDate(),
            p.endDate(),
            PeriodStatus.CLOSED,
            p.expectedIncome(),
            p.lines()));
    assertThrows(
        ConflictException.class,
        () ->
            service.accountExpense("h", new AccountExpense("q1", "cash", money("1.00"), "Gasto")));
    assertThrows(
        ConflictException.class,
        () ->
            service.previewAccountExpense(
                "h", new AccountExpense("q1", "cash", money("1.00"), "Gasto")));
  }

  @Test
  void correctionOfSpentIncomeValidatesOnlyNetResultAndPreservesAudit() {
    var original = service.income("h", new Income("q1", "cash", null, money("100.00"), "Original"));
    var spent =
        service.accountExpense("h", new AccountExpense("q1", "cash", money("100.00"), "Gastado"));
    var c =
        correction(
            replacement(MovementType.INCOME, "200.00", "cash", null, null, null, null, null, null));
    var before = Map.copyOf(accountData);
    var preview = service.previewCorrection("h", original.id(), c);
    assertEquals(money("100.00"), preview.accountEffects().get(0).change());
    assertEquals(before, accountData);
    assertEquals(2, movementData.size());
    var updated = service.correct("h", original.id(), c);
    assertEquals(money("1180.00"), accountData.get("cash").balance());
    assertEquals(money("200.00"), updated.accountEffects().get(0).change());
    assertEquals(money("980.00"), updated.accountEffects().get(0).before());
    assertEquals(original.id(), updated.correction().originalId());
    var undo = movementData.get(updated.correction().reversalId());
    assertEquals(original.id(), undo.reversalOf());
    assertEquals(money("-100.00"), undo.accountEffects().get(0).change());
    var historical = movementData.get(original.id());
    assertEquals(MovementStatus.REVERSED, historical.status());
    assertEquals(original.occurredAt(), historical.occurredAt());
    assertEquals(original.description(), historical.description());
    assertEquals(original.accountEffects(), historical.accountEffects());
    assertEquals(spent, movementData.get(spent.id()));
    assertEquals(updated, MongoMapper.toDomain(MongoMapper.toDocument(updated)));
    assertThrows(ConflictException.class, () -> service.correct("h", original.id(), c));
    assertThrows(
        ConflictException.class,
        () -> service.reverse("h", original.id(), new Reverse("q1", "No")));
    assertThrows(ConflictException.class, () -> service.correct("h", undo.id(), c));
  }

  @Test
  void invalidNetCorrectionLeavesEveryBalanceAndMovementIntact() {
    var original = service.income("h", new Income("q1", "cash", null, money("100.00"), "Ingreso"));
    service.accountExpense("h", new AccountExpense("q1", "cash", money("100.00"), "Gastado"));
    var beforeA = Map.copyOf(accountData);
    var beforeE = Map.copyOf(envelopeData);
    var beforeM = Map.copyOf(movementData);
    var c =
        correction(
            replacement(MovementType.INCOME, "50.00", "cash", null, null, null, null, null, null));
    assertThrows(ValidationException.class, () -> service.previewCorrection("h", original.id(), c));
    assertThrows(ValidationException.class, () -> service.correct("h", original.id(), c));
    assertEquals(beforeA, accountData);
    assertEquals(beforeE, envelopeData);
    assertEquals(beforeM, movementData);
  }

  @Test
  void correctionSupportsEveryFinancialTypeAndReplacementReversal() {
    for (MovementType type :
        List.of(
            MovementType.ALLOCATE,
            MovementType.RELEASE,
            MovementType.REALLOCATE,
            MovementType.EXPENSE,
            MovementType.TRANSFER,
            MovementType.ENVELOPE_TRANSFER)) {
      setup();
      accountData.put(
          "bank",
          new Account("bank", "h", "Banco", AccountType.DIGITAL, money("500.00"), 0L, true));
      envelopeData.put(
          "saving",
          new Envelope(
              "saving",
              "h",
              "Ahorro",
              "bank",
              EnvelopeType.SAVINGS,
              "Ahorro",
              money("0.00"),
              0L,
              true));
      service.income("h", new Income("q1", "cash", null, money("500.00"), "Libre"));
      var beforeA = Map.copyOf(accountData);
      var beforeE = Map.copyOf(envelopeData);
      Movement original;
      MovementReplacement repl;
      switch (type) {
        case ALLOCATE -> {
          original =
              service.allocate("h", new Allocation("q1", "central", money("20.00"), "Original"));
          repl = replacement(type, "30.00", null, "central", null, null, null, null, null);
        }
        case RELEASE -> {
          original =
              service.release("h", new Allocation("q1", "central", money("20.00"), "Original"));
          repl = replacement(type, "30.00", null, "central", null, null, null, null, null);
        }
        case REALLOCATE -> {
          original =
              service.reallocate(
                  "h", new Reallocation("q1", "central", "sams", money("20.00"), "Original"));
          repl = replacement(type, "30.00", null, null, null, null, "central", "sams", null);
        }
        case EXPENSE -> {
          original =
              service.expense("h", new Expense("q1", "central", money("20.00"), "Original", null));
          repl =
              replacement(
                  type,
                  "30.00",
                  null,
                  "central",
                  null,
                  null,
                  null,
                  null,
                  List.of(new Funding("sams", money("30.00"))));
        }
        case TRANSFER -> {
          original =
              service.transfer("h", new Transfer("q1", "cash", "bank", money("20.00"), "Original"));
          repl = replacement(type, "30.00", null, null, "cash", "bank", null, null, null);
        }
        case ENVELOPE_TRANSFER -> {
          original =
              service.transferEnvelope(
                  "h", new EnvelopeTransfer("q1", "central", "saving", money("20.00"), "Original"));
          repl = replacement(type, "30.00", null, null, null, null, "central", "saving", null);
        }
        default -> throw new AssertionError();
      }
      var updated = service.correct("h", original.id(), correction(repl));
      assertEquals(type, updated.type());
      assertEquals(money("30.00"), updated.amount());
      assertEquals("Corregido", updated.description());
      assertEquals(MovementStatus.REVERSED, movementData.get(original.id()).status());
      service.reverse("h", updated.id(), new Reverse("q1", "Cancelar reemplazo"));
      assertEquals(beforeA, accountData, type.toString());
      assertEquals(beforeE, envelopeData, type.toString());
      assertEquals(updated.correction(), movementData.get(updated.id()).correction());
    }
  }

  @Test
  void correctionCanSwitchExpenseBetweenAccountAndEnvelopeWithoutTouchingUnrelatedReserves() {
    service.income("h", new Income("q1", "cash", null, money("100.00"), "Libre"));
    var original =
        service.accountExpense("h", new AccountExpense("q1", "cash", money("30.00"), "Directo"));
    var updated =
        service.correct(
            "h",
            original.id(),
            correction(
                replacement(
                    MovementType.EXPENSE, "50.00", null, "central", null, null, null, null, null)));
    assertEquals(money("1130.00"), accountData.get("cash").balance());
    assertEquals(money("30.00"), envelopeData.get("central").balance());
    assertEquals(money("1000.00"), envelopeData.get("sams").balance());
    var direct =
        service.correct(
            "h",
            updated.id(),
            correction(
                replacement(
                    MovementType.EXPENSE, "60.00", "cash", null, null, null, null, null, null)));
    assertNull(direct.targetEnvelopeId());
    assertEquals(money("1120.00"), accountData.get("cash").balance());
    assertEquals(money("80.00"), envelopeData.get("central").balance());
    assertEquals(updated.id(), direct.correction().originalId());
  }

  @Test
  void correctionCanUseOpenPeriodForHistoricalOriginalButRejectsClosedDestination() {
    var original = service.income("h", new Income("q1", "cash", null, money("100.00"), "Original"));
    var p = periodData.get("q1");
    periodData.put(
        "q1",
        new BudgetPeriod(
            p.id(),
            "h",
            p.startDate(),
            p.endDate(),
            PeriodStatus.CLOSED,
            p.expectedIncome(),
            p.lines()));
    var c =
        correction(
            replacement(MovementType.INCOME, "200.00", "cash", null, null, null, null, null, null));
    assertThrows(ConflictException.class, () -> service.correct("h", original.id(), c));
    periodData.put(
        "q2",
        new BudgetPeriod(
            "q2",
            "h",
            p.startDate().plusMonths(1),
            p.endDate().plusMonths(1),
            PeriodStatus.OPEN,
            p.expectedIncome(),
            p.lines()));
    var updated =
        service.correct("h", original.id(), new CorrectMovement("q2", c.reason(), c.replacement()));
    assertEquals("q2", updated.periodId());
    assertEquals("q1", movementData.get(original.id()).periodId());
    assertEquals(PeriodStatus.CLOSED, periodData.get("q1").status());
    assertEquals(original.occurredAt(), movementData.get(original.id()).occurredAt());
  }

  @Test
  void correctionRejectsDifferentTypeInvalidSourceOpeningAndMalformedFunding() {
    var original = service.income("h", new Income("q1", "cash", null, money("100.00"), "Original"));
    assertThrows(
        ValidationException.class,
        () ->
            service.correct(
                "h",
                original.id(),
                correction(
                    replacement(
                        MovementType.EXPENSE,
                        "10.00",
                        "cash",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))));
    var badSource =
        new MovementReplacement(
            MovementType.INCOME,
            money("200.00"),
            "Corregido",
            "cash",
            "missing",
            null,
            null,
            null,
            null,
            null,
            null);
    assertThrows(
        NotFoundException.class, () -> service.correct("h", original.id(), correction(badSource)));
    var expense =
        service.expense("h", new Expense("q1", "central", money("10.00"), "Original", null));
    var invalid =
        replacement(
            MovementType.EXPENSE,
            "20.00",
            null,
            "central",
            null,
            null,
            null,
            null,
            List.of(new Funding("central", money("5.00"))));
    assertThrows(
        ValidationException.class, () -> service.correct("h", expense.id(), correction(invalid)));
    var duplicate =
        replacement(
            MovementType.EXPENSE,
            "20.00",
            null,
            "central",
            null,
            null,
            null,
            null,
            List.of(
                new Funding("central", money("10.00")), new Funding("central", money("10.00"))));
    assertThrows(
        ValidationException.class, () -> service.correct("h", expense.id(), correction(duplicate)));
    assertThrows(
        ValidationException.class,
        () -> service.correct("h", original.id(), new CorrectMovement("q1", " ", badSource)));
    accountData.put(
        "new", new Account("new", "h", "Nueva", AccountType.DIGITAL, money("0.00"), 0L, true));
    var opening = service.opening("h", new OpeningBalance(null, "new", money("20.00"), "Inicial"));
    assertThrows(
        ConflictException.class,
        () ->
            service.correct(
                "h",
                opening.id(),
                correction(
                    replacement(
                        MovementType.OPENING_BALANCE,
                        "30.00",
                        "new",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))));
  }

  @Test
  void financialHttpSupportsAccountExpenseCorrectionValidationAndStableReplay() throws Exception {
    var idem = new IdempotentMovementServiceTest();
    var mvc =
        MockMvcBuilders.standaloneSetup(new MovementController(service, idem.service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    service.income("h", new Income("q1", "cash", null, money("300.00"), "Libre"));
    String url = "/api/v1/households/h/movements/account-expenses";
    String expense =
        "{\"periodId\":\"q1\",\"accountId\":\"cash\",\"amount\":100,\"description\":\"Original\"}";
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(expense))
        .andExpect(status().isBadRequest());
    mvc.perform(post(url + "/preview").contentType(MediaType.APPLICATION_JSON).content(expense))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.freeBefore").value(300));
    String response =
        mvc.perform(
                post(url)
                    .header("Idempotency-Key", "direct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(expense))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.envelopeEffects.length()").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String replay =
        mvc.perform(
                post(url)
                    .header("Idempotency-Key", "direct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(expense))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(response, replay);
    var original =
        movementData.values().stream()
            .filter(m -> m.type() == MovementType.EXPENSE)
            .findFirst()
            .orElseThrow();
    String correctionUrl = "/api/v1/households/h/movements/" + original.id() + "/corrections";
    String body =
        "{\"periodId\":\"q1\",\"reason\":\"Captura\",\"replacement\":{\"type\":\"ACCOUNT_EXPENSE\",\"accountId\":\"cash\",\"amount\":150,\"description\":\"Corregido\"}}";
    mvc.perform(
            post(correctionUrl + "/preview").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountEffects[0].change").value(-50));
    for (String invalid :
        List.of(
            body.replace("ACCOUNT_EXPENSE", "UNKNOWN"),
            body.replace("150", "-1"),
            body.replace("150", "1.001"),
            body.replace("Captura", ""),
            body.replace("cash", "")))
      mvc.perform(
              post(correctionUrl)
                  .header("Idempotency-Key", "bad")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(invalid))
          .andExpect(status().isBadRequest());
    mvc.perform(post(correctionUrl).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    String corrected =
        mvc.perform(
                post(correctionUrl)
                    .header("Idempotency-Key", "correction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correction.originalId").value(original.id()))
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(
        corrected,
        mvc.perform(
                post(correctionUrl)
                    .header("Idempotency-Key", "correction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
    mvc.perform(
            post(correctionUrl)
                .header("Idempotency-Key", "correction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("150", "160")))
        .andExpect(status().isConflict());
    assertEquals(4, movementData.size());
    assertEquals(money("1230.00"), accountData.get("cash").balance());
    idem.authorized.set(false);
    mvc.perform(
            post(correctionUrl)
                .header("Idempotency-Key", "correction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void newOperationsRequireActiveEditorMembershipIncludingPreviews() {
    var original = service.income("h", new Income("q1", "cash", null, money("100.00"), "Original"));
    var c =
        correction(
            replacement(MovementType.INCOME, "200.00", "cash", null, null, null, null, null, null));
    var expense = new AccountExpense("q1", "cash", money("10.00"), "Directo");
    for (boolean active : new boolean[] {true, false}) {
      testActive = active;
      testRole = active ? MemberRole.VIEWER : MemberRole.EDITOR;
      assertThrows(ForbiddenException.class, () -> service.accountExpense("h", expense));
      assertThrows(ForbiddenException.class, () -> service.previewAccountExpense("h", expense));
      assertThrows(ForbiddenException.class, () -> service.correct("h", original.id(), c));
      assertThrows(
          ForbiddenException.class, () -> service.previewCorrection("h", original.id(), c));
    }
    testActive = true;
    testRole = MemberRole.EDITOR;
    var updated = service.correct("h", original.id(), c);
    assertEquals(money("100.00"), updated.correction().netAccountEffects().get(0).change());
    service.accountExpense("h", expense);
  }

  @Test
  void correctionCanMoveIncomeToAnotherAccountAndRejectsCrossHouseholdReferences() {
    accountData.put(
        "bank", new Account("bank", "h", "Banco", AccountType.DIGITAL, money("0.00"), 0L, true));
    var original = service.income("h", new Income("q1", "cash", null, money("100.00"), "Original"));
    var updated =
        service.correct(
            "h",
            original.id(),
            correction(
                replacement(
                    MovementType.INCOME, "200.00", "bank", null, null, null, null, null, null)));
    assertEquals(money("1080.00"), accountData.get("cash").balance());
    assertEquals(money("200.00"), accountData.get("bank").balance());
    assertEquals(2, updated.correction().netAccountEffects().size());
    accountData.put(
        "other",
        new Account("other", "other", "Ajena", AccountType.DIGITAL, money("0.00"), 0L, true));
    var before = Map.copyOf(movementData);
    assertThrows(
        NotFoundException.class,
        () ->
            service.correct(
                "h",
                updated.id(),
                correction(
                    replacement(
                        MovementType.INCOME,
                        "300.00",
                        "other",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))));
    assertEquals(before, movementData);
    assertEquals(money("200.00"), accountData.get("bank").balance());
  }

  @Test
  void multiEnvelopeExpenseCorrectedToZeroRestoresEveryContributionAndCanBeEditedAgain() {
    var original =
        service.expense(
            "h",
            new Expense(
                "q1",
                "central",
                money("150.00"),
                "Original",
                List.of(
                    new Funding("central", money("80.00")), new Funding("sams", money("70.00")))));
    var cmd =
        correction(
            replacement(
                MovementType.EXPENSE, "0.00", null, "central", null, null, null, null, null));
    var preview = service.previewCorrection("h", original.id(), cmd);
    assertEquals(money("150.00"), preview.accountEffects().get(0).change());
    assertEquals(
        money("80.00"),
        preview.envelopeEffects().stream()
            .filter(e -> e.entityId().equals("central"))
            .findFirst()
            .orElseThrow()
            .change());
    assertEquals(
        money("70.00"),
        preview.envelopeEffects().stream()
            .filter(e -> e.entityId().equals("sams"))
            .findFirst()
            .orElseThrow()
            .change());
    var zero = service.correct("h", original.id(), cmd);
    assertEquals(money("1080.00"), accountData.get("cash").balance());
    assertEquals(money("80.00"), envelopeData.get("central").balance());
    assertEquals(money("1000.00"), envelopeData.get("sams").balance());
    assertEquals(MovementType.EXPENSE, zero.type());
    assertEquals(money("0.00"), zero.amount());
    assertTrue(zero.funding().isEmpty());
    assertTrue(zero.accountEffects().isEmpty());
    assertTrue(zero.envelopeEffects().isEmpty());
    assertEquals(MovementStatus.REVERSED, movementData.get(original.id()).status());
    var response = MovementResponse.from(MongoMapper.toDomain(MongoMapper.toDocument(zero)));
    assertEquals("EXPENSE", response.replacement().type());
    assertEquals("central", response.replacement().targetEnvelopeId());
    assertTrue(response.replacement().fundingSources().isEmpty());
    assertEquals(original.id(), response.correction().originalId());
    var edited =
        service.correct(
            "h",
            zero.id(),
            correction(
                replacement(
                    MovementType.EXPENSE, "20.00", null, "central", null, null, null, null, null)));
    assertEquals(money("1060.00"), accountData.get("cash").balance());
    assertEquals(money("60.00"), envelopeData.get("central").balance());
    assertEquals(zero.id(), edited.correction().originalId());
  }

  @Test
  void zeroDirectExpenseCompensatesOnlyOriginalAndReplaysWithoutChangingReserves() {
    service.income("h", new Income("q1", "cash", null, money("100.00"), "Libre"));
    var original =
        service.accountExpense("h", new AccountExpense("q1", "cash", money("100.00"), "Directo"));
    var cmd =
        correction(
            replacement(MovementType.EXPENSE, "0.00", "cash", null, null, null, null, null, null));
    var before = Map.copyOf(envelopeData);
    var idem = new IdempotentMovementServiceTest();
    var zero =
        idem.service.execute(
            "h",
            "zero",
            "correct:" + original.id(),
            cmd,
            () -> service.correct("h", original.id(), cmd));
    assertEquals(
        zero,
        idem.service.execute(
            "h",
            "zero",
            "correct:" + original.id(),
            cmd,
            () -> {
              throw new AssertionError("Replay");
            }));
    assertEquals(money("1180.00"), accountData.get("cash").balance());
    assertEquals(before, envelopeData);
    var response = MovementResponse.from(zero);
    assertEquals("ACCOUNT_EXPENSE", response.replacement().type());
    assertEquals("cash", response.replacement().accountId());
    assertTrue(zero.funding().isEmpty());
    assertEquals(4, movementData.size());
    service.correct(
        "h",
        zero.id(),
        correction(
            replacement(
                MovementType.EXPENSE, "50.00", "cash", null, null, null, null, null, null)));
    assertEquals(money("1130.00"), accountData.get("cash").balance());
    assertEquals(before, envelopeData);
  }

  @Test
  void zeroCorrectionsOfOtherSixTypesKeepResourcesAndAllowSecondEdit() {
    for (var type :
        List.of(
            MovementType.INCOME,
            MovementType.ALLOCATE,
            MovementType.RELEASE,
            MovementType.REALLOCATE,
            MovementType.TRANSFER,
            MovementType.ENVELOPE_TRANSFER)) {
      setup();
      accountData.put(
          "bank",
          new Account("bank", "h", "Banco", AccountType.DIGITAL, money("500.00"), 0L, true));
      envelopeData.put(
          "saving",
          new Envelope(
              "saving",
              "h",
              "Ahorro",
              "bank",
              EnvelopeType.SAVINGS,
              "Ahorro",
              money("0.00"),
              0L,
              true));
      service.income("h", new Income("q1", "cash", null, money("500.00"), "Libre"));
      incomeData.put(
          "payroll",
          new IncomeSource(
              "payroll", "h", "Nomina", IncomeSourceType.PAYROLL, "cash", money("100.00"), true));
      var beforeA = Map.copyOf(accountData);
      var beforeE = Map.copyOf(envelopeData);
      Movement original;
      MovementReplacement zeroCmd;
      switch (type) {
        case INCOME -> {
          original =
              service.income("h", new Income("q1", "cash", null, money("20.00"), "Original"));
          zeroCmd =
              new MovementReplacement(
                  type,
                  money("0.00"),
                  "Corregido",
                  "cash",
                  "payroll",
                  null,
                  null,
                  null,
                  null,
                  null,
                  null);
        }
        case ALLOCATE -> {
          original =
              service.allocate("h", new Allocation("q1", "central", money("20.00"), "Original"));
          zeroCmd = replacement(type, "0.00", null, "central", null, null, null, null, null);
        }
        case RELEASE -> {
          original =
              service.release("h", new Allocation("q1", "central", money("20.00"), "Original"));
          zeroCmd = replacement(type, "0.00", null, "central", null, null, null, null, null);
        }
        case REALLOCATE -> {
          original =
              service.reallocate(
                  "h", new Reallocation("q1", "central", "sams", money("20.00"), "Original"));
          zeroCmd = replacement(type, "0.00", null, null, null, null, "central", "sams", null);
        }
        case TRANSFER -> {
          original =
              service.transfer("h", new Transfer("q1", "cash", "bank", money("20.00"), "Original"));
          zeroCmd = replacement(type, "0.00", null, null, "cash", "bank", null, null, null);
        }
        case ENVELOPE_TRANSFER -> {
          original =
              service.transferEnvelope(
                  "h", new EnvelopeTransfer("q1", "central", "saving", money("20.00"), "Original"));
          zeroCmd = replacement(type, "0.00", null, null, null, null, "central", "saving", null);
        }
        default -> throw new AssertionError();
      }
      var zero = service.correct("h", original.id(), correction(zeroCmd));
      assertEquals(beforeA, accountData, type.toString());
      assertEquals(beforeE, envelopeData, type.toString());
      var response = MovementResponse.from(MongoMapper.toDomain(MongoMapper.toDocument(zero)));
      assertEquals(type.name(), response.replacement().type());
      assertEquals(zeroCmd.accountId(), response.replacement().accountId());
      assertEquals(zeroCmd.incomeSourceId(), response.replacement().incomeSourceId());
      assertEquals(zeroCmd.envelopeId(), response.replacement().envelopeId());
      assertEquals(zeroCmd.fromAccountId(), response.replacement().fromAccountId());
      assertEquals(zeroCmd.toAccountId(), response.replacement().toAccountId());
      assertEquals(zeroCmd.fromEnvelopeId(), response.replacement().fromEnvelopeId());
      assertEquals(zeroCmd.toEnvelopeId(), response.replacement().toEnvelopeId());
      var next =
          new MovementReplacement(
              type,
              money("10.00"),
              "Segundo",
              zeroCmd.accountId(),
              zeroCmd.incomeSourceId(),
              zeroCmd.envelopeId(),
              zeroCmd.fromAccountId(),
              zeroCmd.toAccountId(),
              zeroCmd.fromEnvelopeId(),
              zeroCmd.toEnvelopeId(),
              null);
      var edited = service.correct("h", zero.id(), correction(next));
      assertEquals(money("10.00"), edited.amount());
      assertEquals(zero.id(), edited.correction().originalId());
    }
  }

  @Test
  void zeroExpenseRejectsAllFundingRowsAndNewMovementsRemainStrictlyPositive() {
    var original =
        service.expense("h", new Expense("q1", "central", money("20.00"), "Original", null));
    var before = Map.copyOf(movementData);
    for (var funding :
        List.of(
            List.of(new Funding("central", money("1.00"))),
            List.of(new Funding("central", money("0.00"))))) {
      assertThrows(
          ValidationException.class,
          () ->
              service.correct(
                  "h",
                  original.id(),
                  correction(
                      replacement(
                          MovementType.EXPENSE,
                          "0.00",
                          null,
                          "central",
                          null,
                          null,
                          null,
                          null,
                          funding))));
    }
    assertEquals(before, movementData);
    assertThrows(
        ValidationException.class,
        () -> service.income("h", new Income("q1", "cash", null, Money.ZERO, "Nuevo")));
    assertThrows(
        ValidationException.class,
        () -> service.allocate("h", new Allocation("q1", "central", Money.ZERO, "Nuevo")));
    assertThrows(
        ValidationException.class,
        () -> service.expense("h", new Expense("q1", "central", Money.ZERO, "Nuevo", List.of())));
    assertThrows(
        ValidationException.class,
        () -> service.accountExpense("h", new AccountExpense("q1", "cash", Money.ZERO, "Nuevo")));
    var zero =
        service.correct(
            "h",
            original.id(),
            correction(
                replacement(
                    MovementType.EXPENSE,
                    "0.00",
                    null,
                    "central",
                    null,
                    null,
                    null,
                    null,
                    List.of())));
    assertTrue(zero.funding().isEmpty());
  }

  @Test
  void zeroCorrectionCannotRemoveSpentIncomeOrFundsBackingReserves() {
    var income = service.income("h", new Income("q1", "cash", null, money("100.00"), "Original"));
    service.accountExpense("h", new AccountExpense("q1", "cash", money("100.00"), "Gastado"));
    var beforeA = Map.copyOf(accountData);
    var beforeM = Map.copyOf(movementData);
    assertThrows(
        ValidationException.class,
        () ->
            service.correct(
                "h",
                income.id(),
                correction(
                    replacement(
                        MovementType.INCOME, "0.00", "cash", null, null, null, null, null, null))));
    assertEquals(beforeA, accountData);
    assertEquals(beforeM, movementData);
    setup();
    var release = service.release("h", new Allocation("q1", "central", money("20.00"), "Original"));
    service.accountExpense("h", new AccountExpense("q1", "cash", money("20.00"), "Gastado"));
    var a = Map.copyOf(accountData);
    var e = Map.copyOf(envelopeData);
    var m = Map.copyOf(movementData);
    assertThrows(
        ValidationException.class,
        () ->
            service.correct(
                "h",
                release.id(),
                correction(
                    replacement(
                        MovementType.RELEASE,
                        "0.00",
                        null,
                        "central",
                        null,
                        null,
                        null,
                        null,
                        null))));
    assertEquals(a, accountData);
    assertEquals(e, envelopeData);
    assertEquals(m, movementData);
  }

  @Test
  void zeroExpenseHttpPreservesAuditResourcesAndRejectsNonemptyFunding() throws Exception {
    var original =
        service.expense(
            "h",
            new Expense(
                "q1",
                "central",
                money("150.00"),
                "Original",
                List.of(
                    new Funding("central", money("80.00")), new Funding("sams", money("70.00")))));
    var idem = new IdempotentMovementServiceTest();
    var mvc =
        MockMvcBuilders.standaloneSetup(new MovementController(service, idem.service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    String url = "/api/v1/households/h/movements/" + original.id() + "/corrections";
    String body =
        "{\"periodId\":\"q1\",\"reason\":\"Cero\",\"replacement\":{\"type\":\"EXPENSE\",\"targetEnvelopeId\":\"central\",\"amount\":0,\"fundingSources\":[]}}";
    for (String amount : List.of("0", "1")) {
      mvc.perform(
              post(url)
                  .header("Idempotency-Key", "zero")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      body.replace(
                          "[]", "[{\"envelopeId\":\"central\",\"amount\":" + amount + "}]")))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post(url)
                .header("Idempotency-Key", "zero")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(0))
        .andExpect(jsonPath("$.type").value("EXPENSE"))
        .andExpect(jsonPath("$.replacement.type").value("EXPENSE"))
        .andExpect(jsonPath("$.replacement.targetEnvelopeId").value("central"))
        .andExpect(jsonPath("$.replacement.fundingSources.length()").value(0))
        .andExpect(jsonPath("$.funding.length()").value(0))
        .andExpect(jsonPath("$.correction.originalId").value(original.id()));
    assertEquals(money("1080.00"), accountData.get("cash").balance());
  }
}

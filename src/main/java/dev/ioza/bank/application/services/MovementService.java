package dev.ioza.bank.application.services;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import dev.ioza.bank.application.commands.SendHouseholdNotificationCommand;
import dev.ioza.bank.application.commands.Transfer;
import dev.ioza.bank.application.ports.in.AccountExpensePreview;
import dev.ioza.bank.application.ports.in.CorrectionPreview;
import dev.ioza.bank.application.ports.in.DonorOption;
import dev.ioza.bank.application.ports.in.ExpensePreview;
import dev.ioza.bank.application.ports.in.Finance;
import dev.ioza.bank.application.ports.in.SendHouseholdNotificationUseCase;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Budgets;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.application.ports.out.Incomes;
import dev.ioza.bank.application.ports.out.Movements;
import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.domain.Money;
import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.BalanceEffect;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.CorrectionLink;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.Funding;
import dev.ioza.bank.domain.model.IncomeSource;
import dev.ioza.bank.domain.model.Movement;
import dev.ioza.bank.domain.model.ReplacementResources;

/**
 * One movement changes all affected persisted balances within one MongoDB
 * transaction.
 */
@Service
public class MovementService implements Finance {

  private final Accounts accounts;

  private final Envelopes envelopes;

  private final Incomes incomes;

  private final Budgets budgets;

  private final Movements movements;

  private final AccessService access;

  private static final Logger log = LoggerFactory.getLogger(MovementService.class);

  private final SendHouseholdNotificationUseCase notifications;
  public MovementService(
      Accounts accounts,
      Envelopes envelopes,
      Incomes incomes,
      Budgets budgets,
      Movements movements,
      AccessService access,
      SendHouseholdNotificationUseCase notifications) {
    this.accounts = accounts;
    this.envelopes = envelopes;
    this.incomes = incomes;
    this.budgets = budgets;
    this.movements = movements;
    this.access = access;
    this.notifications = notifications;
  }

  private static String id() {
    return UUID.randomUUID().toString();
  }

  private BudgetPeriod checkPeriod(String household, String periodId) {
    BudgetPeriod p = budgets
        .getPeriod(periodId)
        .orElseThrow(() -> new NotFoundException("Quincena no encontrada"));
    if (!p.householdId().equals(household) || p.status() != PeriodStatus.OPEN)
      throw new ConflictException("La quincena no está abierta para este hogar");
    return p;
  }

  private void requirePeriod(String household, String periodId) {
    budgets.lockPeriod(checkPeriod(household, periodId).id());
  }

  private Account account(String household, String id) {
    Account a = accounts.find(id).orElseThrow(() -> new NotFoundException("Cuenta no encontrada"));
    if (!a.householdId().equals(household) || !a.active())
      throw new NotFoundException("Cuenta no disponible");
    return a;
  }

  private Envelope envelope(String household, String id) {
    Envelope e = envelopes.find(id).orElseThrow(() -> new NotFoundException("Sobre no encontrado"));
    if (!e.householdId().equals(household) || !e.active())
      throw new NotFoundException("Sobre no disponible");
    return e;
  }

  private final class Changes {
    final String household;
    final Map<String, Account> originalAccounts = new LinkedHashMap<>(),
        updatedAccounts = new LinkedHashMap<>();
    final Map<String, Envelope> originalEnvelopes = new LinkedHashMap<>(),
        updatedEnvelopes = new LinkedHashMap<>();
    Changes(String household) {
      this.household = household;
    }
    Account a(String id) {
      if (!updatedAccounts.containsKey(id)) {
        Account item = account(household, id);
        originalAccounts.put(id, item);
        updatedAccounts.put(id, item);
      }
      return updatedAccounts.get(id);
    }
    Envelope e(String id) {
      if (!updatedEnvelopes.containsKey(id)) {
        Envelope item = envelope(household, id);
        originalEnvelopes.put(id, item);
        updatedEnvelopes.put(id, item);
      }
      a(updatedEnvelopes.get(id).accountId()); // guards concurrent allocations on the same account
      return updatedEnvelopes.get(id);
    }
    void accountDelta(String id, BigDecimal delta) {
      Account old = a(id);
      updatedAccounts.put(id, old.withBalance(Money.normalized(old.balance().add(delta))));
    }
    void envelopeDelta(String id, BigDecimal delta) {
      Envelope old = e(id);
      updatedEnvelopes.put(id, old.withBalance(Money.normalized(old.balance().add(delta))));
    }
    BigDecimal free(String accountId) {
      Account a = a(accountId);
      BigDecimal reserved = envelopes.byAccount(household, accountId).stream()
          .map(e -> updatedEnvelopes.getOrDefault(e.id(), e).balance())
          .reduce(Money.ZERO, BigDecimal::add);
      return a.balance().subtract(reserved);
    }
    void validate() {
      for (Envelope e : updatedEnvelopes.values())
        Money.nonNegative(e.balance(), "sobre " + e.name());
      for (Account a : updatedAccounts.values()) {
        Money.nonNegative(a.balance(), "cuenta " + a.name());
        Money.nonNegative(free(a.id()), "fondos libres de " + a.name());
      }
    }
    List<BalanceEffect> accountEffects() {
      List<BalanceEffect> result = new ArrayList<>();
      for (Account a : updatedAccounts.values()) {
        Account before = originalAccounts.get(a.id());
        result.add(
            new BalanceEffect(
                a.id(), before.balance(), a.balance().subtract(before.balance()), a.balance()));
      }
      return result;
    }
    List<BalanceEffect> envelopeEffects() {
      List<BalanceEffect> result = new ArrayList<>();
      for (Envelope e : updatedEnvelopes.values()) {
        Envelope before = originalEnvelopes.get(e.id());
        result.add(
            new BalanceEffect(
                e.id(), before.balance(), e.balance().subtract(before.balance()), e.balance()));
      }
      return result;
    }
    void persistBalances() {
      for (Account a : updatedAccounts.values())
        accounts.save(a);
      for (Envelope e : updatedEnvelopes.values())
        envelopes.save(e);
    }
    Movement commit(
        String periodId,
        MovementType type,
        BigDecimal amount,
        String description,
        String targetEnvelopeId,
        String incomeSourceId,
        List<Funding> funding,
        String reversalOf) {
      validate();
      var ae = List.copyOf(accountEffects());
      var ee = List.copyOf(envelopeEffects());
      // All reads/writes run under the @Transactional Mongo session; @Version rejects
      // concurrent
      // updates.
      persistBalances();
      return movements.save(
          new Movement(
              id(),
              household,
              periodId,
              type,
              MovementStatus.POSTED,
              amount,
              description,
              targetEnvelopeId,
              incomeSourceId,
              access.username(),
              Instant.now().truncatedTo(ChronoUnit.MILLIS),
              reversalOf,
              funding == null ? List.of() : List.copyOf(funding),
              ae,
              ee));
    }
  }

  @Override
  @Transactional
  public Movement opening(String household, OpeningBalance c) {
    access.requireEditor(household);
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Account account = tx.a(c.accountId());
    if (movements.hasOpening(household, account.id())
        || account.balance().signum() != 0
        || !envelopes.byAccount(household, account.id()).stream()
            .allMatch(e -> e.balance().signum() == 0))
      throw new ConflictException("La cuenta ya tiene dinero; saldo inicial solo con cuenta vacía");
    tx.accountDelta(account.id(), amount);
    return tx.commit(
        null, MovementType.OPENING_BALANCE, amount, c.description(), null, null, null, null);
  }

  @Override
  @Transactional
  public Movement income(String household, Income c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    tx.a(c.accountId());
    if (c.incomeSourceId() != null) {
      IncomeSource s = incomes
          .find(c.incomeSourceId())
          .orElseThrow(() -> new NotFoundException("Fuente no encontrada"));
      if (!s.householdId().equals(household) || !s.active())
        throw new NotFoundException("Fuente no encontrada");
    }
    tx.accountDelta(c.accountId(), amount);
    Movement movement = tx.commit(
        c.periodId(),
        MovementType.INCOME,
        amount,
        c.description(),
        null,
        c.incomeSourceId(),
        null,
        null);

    // Se prepara el mensaje, pero Raven solo se invoca después del commit de MongoDB.
    // Si algo falla preparando la notificación, no debe afectar al ingreso.
    try {
      SendHouseholdNotificationCommand notification = new SendHouseholdNotificationCommand(
          household,
          "bank-income-" + movement.id(),
          "BANK · Nuevo ingreso",
          "%s registró un ingreso de $%,.2f MXN".formatted(
              movement.actor(), movement.amount()),
          Map.of(
              "eventType", "bank.income.created",
              "movementId", movement.id(),
              "householdId", household));

      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          try {
            notifications.send(notification).subscribe(
                ignored -> {},
                error -> log.warn("Error notificando ingreso {}", movement.id(), error),
                () -> log.info("Raven aceptó la notificación del ingreso {}", movement.id()));
          } catch (Exception e) {
            log.warn("No se pudo iniciar la notificación del ingreso {}", movement.id(), e);
          }
        }
      });
    } catch (Exception e) {
      log.warn("No se pudo preparar la notificación del ingreso {}", movement.id(), e);
    }

    return movement;
  }


  @Override
  @Transactional
  public Movement allocate(String household, Allocation c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Envelope e = tx.e(c.envelopeId());
    tx.envelopeDelta(e.id(), amount);
    return tx.commit(
        c.periodId(), MovementType.ALLOCATE, amount, c.description(), e.id(), null, null, null);
  }

  @Override
  @Transactional
  public Movement release(String household, Allocation c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Envelope e = tx.e(c.envelopeId());
    tx.envelopeDelta(e.id(), amount.negate());
    return tx.commit(
        c.periodId(), MovementType.RELEASE, amount, c.description(), e.id(), null, null, null);
  }

  @Override
  @Transactional
  public Movement reallocate(String household, Reallocation c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    if (Objects.equals(c.fromEnvelopeId(), c.toEnvelopeId()))
      throw new ValidationException("El origen y destino deben ser diferentes");
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Envelope from = tx.e(c.fromEnvelopeId()), to = tx.e(c.toEnvelopeId());
    if (!from.accountId().equals(to.accountId()))
      throw new ValidationException("Para sobres en distintas cuentas usa envelope-transfers");
    tx.envelopeDelta(from.id(), amount.negate());
    tx.envelopeDelta(to.id(), amount);
    return tx.commit(
        c.periodId(), MovementType.REALLOCATE, amount, c.description(), to.id(), null, null, null);
  }

  @Override
  @Transactional
  public Movement transfer(String household, Transfer c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    if (Objects.equals(c.fromAccountId(), c.toAccountId()))
      throw new ValidationException("Cuentas idénticas");
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    tx.a(c.fromAccountId());
    tx.a(c.toAccountId());
    if (tx.free(c.fromAccountId()).compareTo(amount) < 0)
      throw new ConflictException(
          "Solo puedes transferir dinero no reservado; usa envelope-transfers para reservas");
    tx.accountDelta(c.fromAccountId(), amount.negate());
    tx.accountDelta(c.toAccountId(), amount);
    return tx.commit(
        c.periodId(), MovementType.TRANSFER, amount, c.description(), null, null, null, null);
  }

  @Override
  @Transactional
  public Movement transferEnvelope(String household, EnvelopeTransfer c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    if (Objects.equals(c.fromEnvelopeId(), c.toEnvelopeId()))
      throw new ValidationException("Sobres idénticos");
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Envelope from = tx.e(c.fromEnvelopeId()), to = tx.e(c.toEnvelopeId());
    if (from.accountId().equals(to.accountId()))
      throw new ValidationException("Misma cuenta: usa reallocations");
    tx.envelopeDelta(from.id(), amount.negate());
    tx.envelopeDelta(to.id(), amount);
    tx.accountDelta(from.accountId(), amount.negate());
    tx.accountDelta(to.accountId(), amount);
    return tx.commit(
        c.periodId(),
        MovementType.ENVELOPE_TRANSFER,
        amount,
        c.description(),
        to.id(),
        null,
        null,
        null);
  }

  @Override
  public ExpensePreview previewExpense(String household, Expense c) {
    access.requireMember(household);
    var amount = Money.positive(c.amount());
    Envelope target = envelope(household, c.targetEnvelopeId());
    BigDecimal shortage = amount.subtract(target.balance()).max(Money.ZERO);
    List<DonorOption> eligible = envelopes.byAccount(household, target.accountId()).stream()
        .filter(e -> e.active() && !e.id().equals(target.id()) && e.balance().signum() > 0)
        .filter(e -> shortage.signum() > 0)
        .map(e -> new DonorOption(e.id(), e.name(), e.balance()))
        .toList();
    return new ExpensePreview(amount, target.balance(), shortage, eligible);
  }

  @Override
  @Transactional
  public Movement expense(String household, Expense c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Envelope target = tx.e(c.targetEnvelopeId());
    List<Funding> sources = c.fundingSources();
    if (sources == null || sources.isEmpty())
      sources = List.of(new Funding(target.id(), amount));
    BigDecimal sum = Money.ZERO;
    Set<String> used = new HashSet<>();
    List<Funding> normalized = new ArrayList<>();
    for (Funding f : sources) {
      if (f == null)
        throw new ValidationException("Fuente de financiamiento obligatoria");
      if (f.envelopeId() == null || !used.add(f.envelopeId()))
        throw new ValidationException("Sobre duplicado o inválido");
      var part = Money.positive(f.amount());
      Envelope source = tx.e(f.envelopeId());
      if (!source.accountId().equals(target.accountId()))
        throw new ValidationException("El gasto solo admite fondos de la misma cuenta de pago");
      tx.envelopeDelta(source.id(), part.negate());
      sum = sum.add(part);
      normalized.add(new Funding(source.id(), part));
    }
    if (sum.compareTo(amount) != 0)
      throw new ValidationException("Las fuentes deben sumar exactamente el gasto");
    tx.accountDelta(target.accountId(), amount.negate());
    return tx.commit(
        c.periodId(),
        MovementType.EXPENSE,
        amount,
        c.description(),
        target.id(),
        null,
        normalized,
        null);
  }

  @Override
  @Transactional
  public Movement reverse(String household, String movementId, Reverse c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    Movement original = movement(household, movementId);
    if (original.status() != MovementStatus.POSTED
        || original.type() == MovementType.REVERSAL
        || movements.hasReversal(original.id()))
      throw new ConflictException("Movimiento ya revertido o no reversible");
    Changes tx = new Changes(household);
    for (BalanceEffect a : original.accountEffects())
      tx.accountDelta(a.entityId(), a.change().negate());
    for (BalanceEffect e : original.envelopeEffects())
      tx.envelopeDelta(e.entityId(), e.change().negate());
    Movement reversed = tx.commit(
        c.periodId(),
        MovementType.REVERSAL,
        original.amount(),
        c.reason(),
        original.targetEnvelopeId(),
        null,
        null,
        original.id());
    movements.save(
        new Movement(
            original.id(),
            original.householdId(),
            original.periodId(),
            original.type(),
            MovementStatus.REVERSED,
            original.amount(),
            original.description(),
            original.targetEnvelopeId(),
            original.incomeSourceId(),
            original.actor(),
            original.occurredAt(),
            original.reversalOf(),
            original.funding(),
            original.accountEffects(),
            original.envelopeEffects(),
            original.correction()));
    return reversed;
  }

  @Override
  @Transactional
  public Movement accountExpense(String household, AccountExpense c) {
    access.requireEditor(household);
    requirePeriod(household, c.periodId());
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    tx.accountDelta(c.accountId(), amount.negate());
    return tx.commit(
        c.periodId(), MovementType.EXPENSE, amount, c.description(), null, null, null, null);
  }

  @Override
  @Transactional(readOnly = true)
  public AccountExpensePreview previewAccountExpense(String household, AccountExpense c) {
    access.requireEditor(household);
    checkPeriod(household, c.periodId());
    var amount = Money.positive(c.amount());
    Changes tx = new Changes(household);
    Account a = tx.a(c.accountId());
    var free = tx.free(a.id());
    tx.accountDelta(a.id(), amount.negate());
    tx.validate();
    return new AccountExpensePreview(
        a.id(), amount, a.balance(), free, a.balance().subtract(amount), free.subtract(amount));
  }

  private record Planned(
      MovementType type,
      BigDecimal amount,
      String description,
      String target,
      String source,
      List<Funding> funding) {
  }
  /**
   * Apply replacement deltas to the same in-memory state as the compensation,
   * then validate once.
   */

  private Planned applyReplacement(Changes tx, MovementReplacement c) {
    var amount = Money.normalized(c.amount());
    Money.nonNegative(amount, "correccion");
    String target = null;
    List<Funding> funding = List.of();
    switch (c.type()) {
      case INCOME -> {
        if (c.incomeSourceId() != null) {
          var source = incomes
              .find(c.incomeSourceId())
              .orElseThrow(() -> new NotFoundException("Fuente no encontrada"));
          if (!source.householdId().equals(tx.household) || !source.active())
            throw new NotFoundException("Fuente no encontrada");
        }
        tx.accountDelta(required(c.accountId()), amount);
      }
      case ALLOCATE, RELEASE -> {
        target = required(c.envelopeId());
        tx.envelopeDelta(target, c.type() == MovementType.ALLOCATE ? amount : amount.negate());
      }
      case REALLOCATE, ENVELOPE_TRANSFER -> {
        String fromId = required(c.fromEnvelopeId()), toId = required(c.toEnvelopeId());
        if (fromId.equals(toId))
          throw new ValidationException("Sobres identicos");
        Envelope from = tx.e(fromId), to = tx.e(toId);
        boolean same = from.accountId().equals(to.accountId());
        if (same != (c.type() == MovementType.REALLOCATE))
          throw new ValidationException(
              "Tipo de transferencia incompatible con las cuentas de los sobres");
        tx.envelopeDelta(fromId, amount.negate());
        tx.envelopeDelta(toId, amount);
        if (!same) {
          tx.accountDelta(from.accountId(), amount.negate());
          tx.accountDelta(to.accountId(), amount);
        }
        target = toId;
      }
      case TRANSFER -> {
        String fromId = required(c.fromAccountId()), toId = required(c.toAccountId());
        if (fromId.equals(toId))
          throw new ValidationException("Cuentas identicas");
        tx.accountDelta(fromId, amount.negate());
        tx.accountDelta(toId, amount);
      }
      case EXPENSE -> {
        if (c.accountId() != null) {
          if (c.envelopeId() != null || c.fundingSources() != null)
            throw new ValidationException("Gasto directo no admite sobres");
          tx.accountDelta(required(c.accountId()), amount.negate());
        } else {
          target = required(c.envelopeId());
          Envelope e = tx.e(target);
          var sources = c.fundingSources();
          if (amount.signum() == 0) {
            if (sources != null && !sources.isEmpty())
              throw new ValidationException(
                  "Un gasto corregido a cero requiere financiacion vacia");
            sources = List.of();
          } else if (sources == null || sources.isEmpty())
            sources = List.of(new Funding(target, amount));
          var sum = Money.ZERO;
          Set<String> seen = new HashSet<>();
          List<Funding> normalized = new ArrayList<>();
          for (Funding f : sources) {
            if (f == null || !seen.add(required(f.envelopeId())))
              throw new ValidationException("Sobre duplicado o invalido");
            var part = Money.positive(f.amount());
            if (!tx.e(f.envelopeId()).accountId().equals(e.accountId()))
              throw new ValidationException(
                  "El gasto solo admite fondos de la misma cuenta de pago");
            tx.envelopeDelta(f.envelopeId(), part.negate());
            normalized.add(new Funding(f.envelopeId(), part));
            sum = sum.add(part);
          }
          if (sum.compareTo(amount) != 0)
            throw new ValidationException("Las fuentes deben sumar exactamente el gasto");
          funding = List.copyOf(normalized);
          tx.accountDelta(e.accountId(), amount.negate());
        }
      }
      default -> throw new ValidationException("Tipo no corregible");
    }
    return new Planned(
        c.type(),
        amount,
        c.description(),
        target,
        c.type() == MovementType.INCOME ? c.incomeSourceId() : null,
        funding);
  }

  private String required(String value) {
    if (value == null || value.isBlank())
      throw new ValidationException("Referencia obligatoria");
    return value;
  }

  private record CorrectionPlan(
      Changes tx,
      Movement original,
      Planned replacement,
      List<BalanceEffect> undoAccounts,
      List<BalanceEffect> undoEnvelopes) {
  }

  private CorrectionPlan planCorrection(
      String household, String movementId, CorrectMovement c, boolean lock) {
    access.requireEditor(household);
    if (lock)
      requirePeriod(household, c.periodId());
    else
      checkPeriod(household, c.periodId());
    if (c.reason() == null
        || c.reason().isBlank()
        || c.replacement() == null
        || c.replacement().type() == null)
      throw new ValidationException("Motivo y reemplazo obligatorios");
    Movement original = movement(household, movementId);
    if (original.status() != MovementStatus.POSTED
        || original.type() == MovementType.REVERSAL
        || original.type() == MovementType.OPENING_BALANCE
        || movements.hasReversal(original.id()))
      throw new ConflictException("Movimiento ya revertido o no corregible");
    if (original.type() != c.replacement().type())
      throw new ValidationException("La correccion conserva el tipo original");
    Changes tx = new Changes(household);
    for (BalanceEffect a : original.accountEffects())
      tx.accountDelta(a.entityId(), a.change().negate());
    for (BalanceEffect e : original.envelopeEffects())
      tx.envelopeDelta(e.entityId(), e.change().negate());
    var undoA = List.copyOf(tx.accountEffects());
    var undoE = List.copyOf(tx.envelopeEffects());
    Planned planned = applyReplacement(tx, c.replacement());
    tx.validate();
    return new CorrectionPlan(tx, original, planned, undoA, undoE);
  }

  private List<BalanceEffect> replacementEffects(
      List<BalanceEffect> net, List<BalanceEffect> undo) {
    Map<String, BigDecimal> inverse = new LinkedHashMap<>();
    for (var e : undo)
      inverse.put(e.entityId(), e.change());
    return net.stream()
        .map(
            e -> {
              var change = inverse.getOrDefault(e.entityId(), Money.ZERO);
              return new BalanceEffect(
                  e.entityId(), e.before().add(change), e.change().subtract(change), e.after());
            })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CorrectionPreview previewCorrection(
      String household, String movementId, CorrectMovement c) {
    var plan = planCorrection(household, movementId, c, false);
    return new CorrectionPreview(
        movementId,
        List.copyOf(plan.tx().accountEffects()),
        List.copyOf(plan.tx().envelopeEffects()));
  }

  @Override
  @Transactional
  public Movement correct(String household, String movementId, CorrectMovement c) {
    var plan = planCorrection(household, movementId, c, true);
    var tx = plan.tx();
    var original = plan.original();
    var replacement = plan.replacement();
    String reversalId = id();
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    // Only final, validated balances are ever persisted; journal compensation
    // balances are virtual.
    var replacementEnvelopeEffects = replacementEffects(tx.envelopeEffects(), plan.undoEnvelopes()).stream()
        .filter(e -> e.change().signum() != 0)
        .toList();
    Set<String> guardedAccounts = new HashSet<>();
    for (var e : replacementEnvelopeEffects)
      guardedAccounts.add(tx.updatedEnvelopes.get(e.entityId()).accountId());
    var replacementAccountEffects = replacementEffects(tx.accountEffects(), plan.undoAccounts()).stream()
        .filter(e -> e.change().signum() != 0 || guardedAccounts.contains(e.entityId()))
        .toList();
    tx.persistBalances();
    movements.save(
        new Movement(
            reversalId,
            household,
            c.periodId(),
            MovementType.REVERSAL,
            MovementStatus.POSTED,
            original.amount(),
            c.reason(),
            original.targetEnvelopeId(),
            null,
            access.username(),
            now,
            original.id(),
            List.of(),
            plan.undoAccounts(),
            plan.undoEnvelopes()));
    movements.save(
        new Movement(
            original.id(),
            original.householdId(),
            original.periodId(),
            original.type(),
            MovementStatus.REVERSED,
            original.amount(),
            original.description(),
            original.targetEnvelopeId(),
            original.incomeSourceId(),
            original.actor(),
            original.occurredAt(),
            original.reversalOf(),
            original.funding(),
            original.accountEffects(),
            original.envelopeEffects(),
            original.correction()));
    return movements.save(
        new Movement(
            id(),
            household,
            c.periodId(),
            replacement.type(),
            MovementStatus.POSTED,
            replacement.amount(),
            replacement.description(),
            replacement.target(),
            replacement.source(),
            access.username(),
            now,
            null,
            replacement.funding(),
            replacementAccountEffects,
            replacementEnvelopeEffects,
            new CorrectionLink(
                original.id(),
                reversalId,
                c.reason(),
                List.copyOf(tx.accountEffects()),
                List.copyOf(tx.envelopeEffects()),
                new ReplacementResources(
                    c.replacement().accountId(),
                    c.replacement().envelopeId(),
                    c.replacement().fromAccountId(),
                    c.replacement().toAccountId(),
                    c.replacement().fromEnvelopeId(),
                    c.replacement().toEnvelopeId(),
                    c.replacement().incomeSourceId()))));
  }

  @Override
  public List<Movement> movements(String household) {
    access.requireMember(household);
    return movements.recent(household);
  }

  @Override
  public List<Movement> movements(String household, MovementQuery query) {
    access.requireMember(household);
    return movements.search(household, query);
  }

  @Override
  public Movement movement(String household, String id) {
    access.requireMember(household);
    Movement m = movements.find(id).orElseThrow(() -> new NotFoundException("Movimiento no encontrado"));
    if (!m.householdId().equals(household))
      throw new NotFoundException("Movimiento no encontrado");
    return m;
  }
}

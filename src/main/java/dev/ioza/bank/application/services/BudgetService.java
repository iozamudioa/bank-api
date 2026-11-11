package dev.ioza.bank.application.services;

import dev.ioza.bank.application.commands.CreatePeriod;
import dev.ioza.bank.application.commands.SaveBudget;
import dev.ioza.bank.application.commands.UpsertPeriodBudgetLine;
import dev.ioza.bank.application.commands.UpsertTemplateBudgetLine;
import dev.ioza.bank.application.ports.in.Budget;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Budgets;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.domain.Money;
import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.BudgetLine;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import dev.ioza.bank.domain.model.Envelope;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService implements Budget {
  private final Budgets budgets;
  private final Envelopes envelopes;
  private final AccessService access;
  private final Accounts accounts;

  public BudgetService(
      Budgets budgets, Envelopes envelopes, AccessService access, Accounts accounts) {
    this.budgets = budgets;
    this.envelopes = envelopes;
    this.access = access;
    this.accounts = accounts;
  }

  @Override
  @Transactional
  public BudgetTemplate upsertTemplateBudgetLine(String householdId, UpsertTemplateBudgetLine cmd) {
    access.requireEditor(householdId);
    budgets.lockSchedule(householdId);
    if (cmd.envelopeId() == null || cmd.envelopeId().isBlank())
      throw new ValidationException("Sobre obligatorio en el plan");
    Envelope envelope =
        envelopes
            .find(cmd.envelopeId())
            .orElseThrow(() -> new NotFoundException("Sobre no encontrado"));
    if (!envelope.householdId().equals(householdId) || !envelope.active())
      throw new ValidationException("Sobre no disponible");
    var account =
        accounts
            .find(envelope.accountId())
            .orElseThrow(() -> new NotFoundException("Cuenta no encontrada"));
    if (!account.householdId().equals(householdId) || !account.active())
      throw new ValidationException("Cuenta no disponible");
    var amount = Money.normalized(cmd.amount());
    Money.nonNegative(amount, "presupuesto habitual");
    BudgetTemplate template =
        budgets
            .getTemplate(householdId)
            .orElseGet(
                () ->
                    new BudgetTemplate(
                        UUID.randomUUID().toString(), householdId, Money.ZERO, List.of()));
    return budgets.saveTemplate(template.withLine(envelope.id(), amount));
  }

  @Override
  @Transactional
  public BudgetPeriod upsertPeriodBudgetLine(
      String householdId, String periodId, UpsertPeriodBudgetLine cmd) {
    access.requireEditor(householdId);
    budgets.lockPeriod(periodId);
    BudgetPeriod p = getPeriod(householdId, periodId);
    if (p.status() != PeriodStatus.OPEN) throw new ConflictException("La quincena no esta abierta");
    if (cmd.envelopeId() == null || cmd.envelopeId().isBlank())
      throw new ValidationException("Sobre obligatorio en el plan");
    Envelope envelope =
        envelopes
            .find(cmd.envelopeId())
            .orElseThrow(() -> new NotFoundException("Sobre no encontrado"));
    if (!envelope.householdId().equals(householdId) || !envelope.active())
      throw new ValidationException("Sobre no disponible");
    var account =
        accounts
            .find(envelope.accountId())
            .orElseThrow(() -> new NotFoundException("Cuenta no encontrada"));
    if (!account.householdId().equals(householdId) || !account.active())
      throw new ValidationException("Cuenta no disponible");
    var amount = Money.normalized(cmd.amount());
    Money.nonNegative(amount, "presupuesto");
    List<BudgetLine> lines = new ArrayList<>(p.lines());
    int index = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).envelopeId().equals(envelope.id())) {
        index = i;
        break;
      }
    }
    BudgetLine line = new BudgetLine(envelope.id(), amount);
    if (index < 0) lines.add(line);
    else lines.set(index, line);
    return budgets.savePeriod(
        new BudgetPeriod(
            p.id(),
            p.householdId(),
            p.startDate(),
            p.endDate(),
            p.status(),
            p.expectedIncome(),
            List.copyOf(lines)));
  }

  @Override
  @Transactional
  public BudgetTemplate saveTemplate(String householdId, SaveBudget cmd) {
    access.requireEditor(householdId);
    budgets.lockSchedule(householdId);
    if (cmd.allocations() == null)
      throw new ValidationException("Indica las asignaciones del presupuesto");
    Set<String> seen = new HashSet<>();
    List<BudgetLine> lines = new ArrayList<>();
    for (BudgetLine line : cmd.allocations()) {
      if (line == null || line.envelopeId() == null || line.envelopeId().isBlank())
        throw new ValidationException("Sobre obligatorio en el plan");
      if (!seen.add(line.envelopeId())) throw new ValidationException("Sobre duplicado en el plan");
      Envelope envelope =
          envelopes
              .find(line.envelopeId())
              .orElseThrow(() -> new NotFoundException("Sobre no encontrado"));
      if (!envelope.householdId().equals(householdId) || !envelope.active())
        throw new ValidationException("Sobre no disponible");
      var amount = Money.normalized(line.amount());
      Money.nonNegative(amount, "presupuesto");
      lines.add(new BudgetLine(envelope.id(), amount));
    }
    var expected = Money.normalized(cmd.expectedIncome());
    Money.nonNegative(expected, "ingreso esperado");
    BudgetTemplate old = budgets.getTemplate(householdId).orElse(null);
    return budgets.saveTemplate(
        new BudgetTemplate(
            old == null ? UUID.randomUUID().toString() : old.id(),
            householdId,
            expected,
            List.copyOf(lines)));
  }

  @Override
  public BudgetTemplate template(String householdId) {
    access.requireMember(householdId);
    return budgets
        .getTemplate(householdId)
        .orElseThrow(() -> new NotFoundException("No hay plantilla de presupuesto"));
  }

  @Override
  @Transactional
  public BudgetPeriod createPeriod(String householdId, CreatePeriod cmd) {
    access.requireEditor(householdId);
    budgets.lockSchedule(householdId);
    if (cmd.startDate() == null || cmd.endDate() == null || cmd.endDate().isBefore(cmd.startDate()))
      throw new ValidationException("Rango de fechas inválido");
    for (BudgetPeriod p : budgets.periods(householdId)) {
      if (!cmd.endDate().isBefore(p.startDate()) && !cmd.startDate().isAfter(p.endDate()))
        throw new ConflictException("Ya existe una quincena que se traslapa");
    }
    BudgetTemplate template = template(householdId);
    return budgets.savePeriod(
        new BudgetPeriod(
            UUID.randomUUID().toString(),
            householdId,
            cmd.startDate(),
            cmd.endDate(),
            cmd.open() ? PeriodStatus.OPEN : PeriodStatus.DRAFT,
            template.expectedIncome(),
            List.copyOf(template.lines())));
  }

  @Override
  public BudgetPeriod getPeriod(String householdId, String periodId) {
    access.requireMember(householdId);
    BudgetPeriod period =
        budgets
            .getPeriod(periodId)
            .orElseThrow(() -> new NotFoundException("Quincena no encontrada"));
    if (!period.householdId().equals(householdId))
      throw new NotFoundException("Quincena no encontrada");
    return period;
  }

  @Override
  public List<BudgetPeriod> periods(String householdId) {
    access.requireMember(householdId);
    return budgets.periods(householdId);
  }

  @Override
  @Transactional
  public BudgetPeriod close(String householdId, String periodId) {
    access.requireEditor(householdId);
    budgets.lockPeriod(periodId);
    BudgetPeriod p = getPeriod(householdId, periodId);
    if (p.status() != PeriodStatus.OPEN) throw new ConflictException("La quincena no está abierta");
    return budgets.savePeriod(
        new BudgetPeriod(
            p.id(),
            p.householdId(),
            p.startDate(),
            p.endDate(),
            PeriodStatus.CLOSED,
            p.expectedIncome(),
            p.lines()));
  }
}

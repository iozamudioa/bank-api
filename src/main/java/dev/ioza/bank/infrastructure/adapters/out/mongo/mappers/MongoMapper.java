package dev.ioza.bank.infrastructure.adapters.out.mongo.mappers;

import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.BalanceEffect;
import dev.ioza.bank.domain.model.BudgetLine;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import dev.ioza.bank.domain.model.CorrectionLink;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.Funding;
import dev.ioza.bank.domain.model.Household;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.domain.model.IncomeSource;
import dev.ioza.bank.domain.model.Movement;
import dev.ioza.bank.domain.model.ReplacementResources;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.AccountDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BalanceEffectDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BudgetLineDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BudgetPeriodDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.BudgetTemplateDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.CorrectionLinkDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.EnvelopeDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.FundingDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.HouseholdDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.HouseholdMemberDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.IncomeSourceDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.MovementDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.ReplacementResourcesDoc;

public final class MongoMapper {
  private MongoMapper() {}

  public static HouseholdDoc toDocument(Household x) {
    return new HouseholdDoc(x.id(), x.name(), x.currency(), x.timezone(), x.active());
  }

  public static Household toDomain(HouseholdDoc x) {
    return new Household(x.id(), x.name(), x.currency(), x.timezone(), x.active());
  }

  public static HouseholdMemberDoc toDocument(HouseholdMember x) {
    return new HouseholdMemberDoc(
        x.id(), x.householdId(), x.username(), x.displayName(), x.role(), x.active());
  }

  public static HouseholdMember toDomain(HouseholdMemberDoc x) {
    return new HouseholdMember(
        x.id(), x.householdId(), x.username(), x.displayName(), x.role(), x.active());
  }

  public static AccountDoc toDocument(Account x) {
    return new AccountDoc(
        x.id(), x.householdId(), x.name(), x.type(), x.balance(), x.version(), x.active());
  }

  public static Account toDomain(AccountDoc x) {
    return new Account(
        x.id(), x.householdId(), x.name(), x.type(), x.balance(), x.version(), x.active());
  }

  public static EnvelopeDoc toDocument(Envelope x) {
    return new EnvelopeDoc(
        x.id(),
        x.householdId(),
        x.name(),
        x.accountId(),
        x.type(),
        x.category(),
        x.balance(),
        x.version(),
        x.active());
  }

  public static Envelope toDomain(EnvelopeDoc x) {
    return new Envelope(
        x.id(),
        x.householdId(),
        x.name(),
        x.accountId(),
        x.type(),
        x.category(),
        x.balance(),
        x.version(),
        x.active());
  }

  public static IncomeSourceDoc toDocument(IncomeSource x) {
    return new IncomeSourceDoc(
        x.id(),
        x.householdId(),
        x.name(),
        x.type(),
        x.defaultAccountId(),
        x.expectedAmount(),
        x.active());
  }

  public static IncomeSource toDomain(IncomeSourceDoc x) {
    return new IncomeSource(
        x.id(),
        x.householdId(),
        x.name(),
        x.type(),
        x.defaultAccountId(),
        x.expectedAmount(),
        x.active());
  }

  public static BudgetTemplateDoc toDocument(BudgetTemplate x) {
    return new BudgetTemplateDoc(
        x.id(),
        x.householdId(),
        x.expectedIncome(),
        x.lines().stream().map(MongoMapper::toDocument).toList());
  }

  public static BudgetTemplate toDomain(BudgetTemplateDoc x) {
    return new BudgetTemplate(
        x.id(),
        x.householdId(),
        x.expectedIncome(),
        x.lines().stream().map(MongoMapper::toDomain).toList());
  }

  public static BudgetPeriodDoc toDocument(BudgetPeriod x) {
    return new BudgetPeriodDoc(
        x.id(),
        x.householdId(),
        x.startDate(),
        x.endDate(),
        x.status(),
        x.expectedIncome(),
        x.lines().stream().map(MongoMapper::toDocument).toList());
  }

  public static BudgetPeriod toDomain(BudgetPeriodDoc x) {
    return new BudgetPeriod(
        x.id(),
        x.householdId(),
        x.startDate(),
        x.endDate(),
        x.status(),
        x.expectedIncome(),
        x.lines().stream().map(MongoMapper::toDomain).toList());
  }

  public static MovementDoc toDocument(Movement x) {
    return new MovementDoc(
        x.id(),
        x.householdId(),
        x.periodId(),
        x.type(),
        x.status(),
        x.amount(),
        x.description(),
        x.targetEnvelopeId(),
        x.incomeSourceId(),
        x.actor(),
        x.occurredAt(),
        x.reversalOf(),
        x.funding().stream().map(MongoMapper::toDocument).toList(),
        x.accountEffects().stream().map(MongoMapper::toDocument).toList(),
        x.envelopeEffects().stream().map(MongoMapper::toDocument).toList(),
        x.correction() == null
            ? null
            : new CorrectionLinkDoc(
                x.correction().originalId(),
                x.correction().reversalId(),
                x.correction().reason(),
                x.correction().netAccountEffects().stream().map(MongoMapper::toDocument).toList(),
                x.correction().netEnvelopeEffects().stream().map(MongoMapper::toDocument).toList(),
                toDocument(x.correction().replacement())));
  }

  public static Movement toDomain(MovementDoc x) {
    return new Movement(
        x.id(),
        x.householdId(),
        x.periodId(),
        x.type(),
        x.status(),
        x.amount(),
        x.description(),
        x.targetEnvelopeId(),
        x.incomeSourceId(),
        x.actor(),
        x.occurredAt(),
        x.reversalOf(),
        x.funding().stream().map(MongoMapper::toDomain).toList(),
        x.accountEffects().stream().map(MongoMapper::toDomain).toList(),
        x.envelopeEffects().stream().map(MongoMapper::toDomain).toList(),
        x.correction() == null
            ? null
            : new CorrectionLink(
                x.correction().originalId(),
                x.correction().reversalId(),
                x.correction().reason(),
                x.correction().netAccountEffects().stream().map(MongoMapper::toDomain).toList(),
                x.correction().netEnvelopeEffects().stream().map(MongoMapper::toDomain).toList(),
                toDomain(x.correction().replacement())));
  }

  public static BudgetLineDoc toDocument(BudgetLine x) {
    return new BudgetLineDoc(x.envelopeId(), x.amount());
  }

  public static BudgetLine toDomain(BudgetLineDoc x) {
    return new BudgetLine(x.envelopeId(), x.amount());
  }

  public static FundingDoc toDocument(Funding x) {
    return new FundingDoc(x.envelopeId(), x.amount());
  }

  public static Funding toDomain(FundingDoc x) {
    return new Funding(x.envelopeId(), x.amount());
  }

  public static BalanceEffectDoc toDocument(BalanceEffect x) {
    return new BalanceEffectDoc(x.entityId(), x.before(), x.change(), x.after());
  }

  public static BalanceEffect toDomain(BalanceEffectDoc x) {
    return new BalanceEffect(x.entityId(), x.before(), x.change(), x.after());
  }

  public static ReplacementResourcesDoc toDocument(ReplacementResources r) {
    return r == null
        ? null
        : new ReplacementResourcesDoc(
            r.accountId(),
            r.envelopeId(),
            r.fromAccountId(),
            r.toAccountId(),
            r.fromEnvelopeId(),
            r.toEnvelopeId(),
            r.incomeSourceId());
  }

  public static ReplacementResources toDomain(ReplacementResourcesDoc r) {
    return r == null
        ? null
        : new ReplacementResources(
            r.accountId(),
            r.envelopeId(),
            r.fromAccountId(),
            r.toAccountId(),
            r.fromEnvelopeId(),
            r.toEnvelopeId(),
            r.incomeSourceId());
  }
}

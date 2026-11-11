package dev.ioza.bank.infrastructure.adapters.out.mongo;

import dev.ioza.bank.application.ports.out.Budgets;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.BudgetTemplate;
import dev.ioza.bank.infrastructure.adapters.out.mongo.mappers.MongoMapper;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.BudgetPeriodMongoRepository;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.BudgetTemplateMongoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class BudgetMongoAdapter implements Budgets {
  private final BudgetTemplateMongoRepository templates;
  private final BudgetPeriodMongoRepository periods;
  private final MongoTemplate mongo;

  public BudgetMongoAdapter(
      BudgetTemplateMongoRepository templates,
      BudgetPeriodMongoRepository periods,
      MongoTemplate mongo) {
    this.templates = templates;
    this.periods = periods;
    this.mongo = mongo;
  }

  public void lockPeriod(String periodId) {
    mongo.updateFirst(
        Query.query(Criteria.where("_id").is(periodId)),
        new Update().inc("revision", 1),
        "budget_periods");
  }

  public void lockSchedule(String householdId) {
    mongo.updateFirst(
        Query.query(Criteria.where("_id").is(householdId)),
        new Update().inc("revision", 1),
        "households");
  }

  public BudgetTemplate saveTemplate(BudgetTemplate x) {
    return MongoMapper.toDomain(templates.save(MongoMapper.toDocument(x)));
  }

  public Optional<BudgetTemplate> getTemplate(String householdId) {
    return templates.findByHouseholdId(householdId).map(MongoMapper::toDomain);
  }

  public BudgetPeriod savePeriod(BudgetPeriod x) {
    return MongoMapper.toDomain(periods.save(MongoMapper.toDocument(x)));
  }

  public Optional<BudgetPeriod> getPeriod(String id) {
    return periods.findById(id).map(MongoMapper::toDomain);
  }

  public Optional<BudgetPeriod> findStart(String householdId, LocalDate date) {
    return periods.findByHouseholdIdAndStartDate(householdId, date).map(MongoMapper::toDomain);
  }

  public List<BudgetPeriod> periods(String householdId) {
    return periods.findByHouseholdIdOrderByStartDateDesc(householdId).stream()
        .map(MongoMapper::toDomain)
        .toList();
  }
}

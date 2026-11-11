package dev.ioza.bank.application.services;

import dev.ioza.bank.application.ports.in.AccountBalance;
import dev.ioza.bank.application.ports.in.Dashboard;
import dev.ioza.bank.application.ports.in.DashboardView;
import dev.ioza.bank.application.ports.out.Accounts;
import dev.ioza.bank.application.ports.out.Envelopes;
import dev.ioza.bank.domain.Money;
import dev.ioza.bank.domain.model.Account;
import dev.ioza.bank.domain.model.Envelope;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService implements Dashboard {
  private final Accounts accounts;
  private final Envelopes envelopes;
  private final AccessService access;

  public DashboardService(Accounts accounts, Envelopes envelopes, AccessService access) {
    this.accounts = accounts;
    this.envelopes = envelopes;
    this.access = access;
  }

  @Override
  public DashboardView get(String householdId) {
    access.requireMember(householdId);
    var allEnvelopes = envelopes.all(householdId);
    BigDecimal total = Money.ZERO, reserved = Money.ZERO, free = Money.ZERO;
    List<AccountBalance> result = new ArrayList<>();
    for (Account a : accounts.all(householdId)) {
      BigDecimal earmarked =
          allEnvelopes.stream()
              .filter(e -> e.accountId().equals(a.id()))
              .map(Envelope::balance)
              .reduce(Money.ZERO, BigDecimal::add);
      result.add(
          new AccountBalance(
              a.id(),
              a.name(),
              a.type().name(),
              a.balance(),
              earmarked,
              a.balance().subtract(earmarked)));
      total = total.add(a.balance());
      reserved = reserved.add(earmarked);
      free = free.add(a.balance().subtract(earmarked));
    }
    return new DashboardView(total, reserved, free, result);
  }
}

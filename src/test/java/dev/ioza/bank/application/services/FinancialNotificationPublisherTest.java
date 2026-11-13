package dev.ioza.bank.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ioza.bank.application.commands.SendHouseholdNotificationCommand;
import dev.ioza.bank.domain.enums.EnvelopeType;
import dev.ioza.bank.domain.model.Envelope;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

class FinancialNotificationPublisherTest {
  private final List<SendHouseholdNotificationCommand> sent = new ArrayList<>();
  private final FinancialNotificationPublisher publisher =
      new FinancialNotificationPublisher(command -> {
        sent.add(command);
        return Mono.empty();
      });

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }

  private Envelope envelope() {
    return new Envelope("e1", "home", "Ahorro", "cash", EnvelopeType.SAVINGS,
        "Familia", BigDecimal.ZERO, null, true);
  }

  @Test
  void doesNotNotifyWithoutAnActiveTransaction() {
    publisher.envelopeCreated(envelope(), "olda");
    assertTrue(sent.isEmpty());
  }

  @Test
  void notifiesOnlyAfterCommit() {
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    publisher.envelopeCreated(envelope(), "olda");
    assertTrue(sent.isEmpty());
    assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations())
      synchronization.afterCommit();

    assertEquals(1, sent.size());
    assertEquals("bank-envelope-created-e1", sent.get(0).notificationId());
    assertEquals("bank.envelope.created", sent.get(0).metadata().get("eventType"));
  }

  @Test
  void doesNotNotifyOnRollback() {
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    publisher.envelopeCreated(envelope(), "olda");

    for (TransactionSynchronization synchronization :
        TransactionSynchronizationManager.getSynchronizations())
      synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    assertTrue(sent.isEmpty());
  }
}

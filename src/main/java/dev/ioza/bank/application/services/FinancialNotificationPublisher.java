package dev.ioza.bank.application.services;

import dev.ioza.bank.application.commands.SendHouseholdNotificationCommand;
import dev.ioza.bank.application.ports.in.SendHouseholdNotificationUseCase;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.model.Envelope;
import dev.ioza.bank.domain.model.Movement;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Best-effort Web Push after a successfully committed financial operation.
 * Never sends from an uncommitted transaction and never blocks its result on Raven.
 */
@Component
public class FinancialNotificationPublisher {
  private static final Logger log = LoggerFactory.getLogger(FinancialNotificationPublisher.class);
  private final SendHouseholdNotificationUseCase notifications;

  public FinancialNotificationPublisher(SendHouseholdNotificationUseCase notifications) {
    this.notifications = notifications;
  }

  public void envelopeCreated(Envelope envelope, String actor) {
    try {
      schedule(new SendHouseholdNotificationCommand(
          envelope.householdId(),
          "bank-envelope-created-" + envelope.id(),
          "BANK · Nuevo sobre",
          actor + " creó el sobre \"" + envelope.name() + "\"",
          Map.of(
              "eventType", "bank.envelope.created",
              "envelopeId", envelope.id(),
              "householdId", envelope.householdId())));
    } catch (Exception error) {
      log.warn("No se pudo preparar aviso de sobre {}", envelope.id(), error);
    }
  }

  public void movement(Movement movement) {
    movement(movement, false);
  }

  public void corrected(Movement movement) {
    movement(movement, true);
  }

  private void movement(Movement movement, boolean correction) {
    try {
      String event = correction
          ? "bank.movement.corrected"
          : switch (movement.type()) {
            case INCOME -> "bank.income.created";
            case EXPENSE -> "bank.expense.created";
            case TRANSFER -> "bank.transfer.created";
            case ENVELOPE_TRANSFER -> "bank.envelope-transfer.created";
            case REALLOCATE -> "bank.reallocation.created";
            case ALLOCATE -> "bank.allocation.created";
            case RELEASE -> "bank.release.created";
            case OPENING_BALANCE -> "bank.opening-balance.created";
            case REVERSAL -> "bank.movement.reversed";
          };
      String action = switch (movement.type()) {
        case INCOME -> "registró un ingreso de";
        case EXPENSE -> "registró un gasto de";
        case TRANSFER -> "transfirió";
        case ENVELOPE_TRANSFER -> "transfirió entre sobres";
        case REALLOCATE -> "reasignó";
        case ALLOCATE -> "apartó";
        case RELEASE -> "liberó";
        case OPENING_BALANCE -> "registró un saldo inicial de";
        case REVERSAL -> "revirtió un movimiento por";
      };
      String title = correction
          ? "BANK · Movimiento corregido"
          : switch (movement.type()) {
            case INCOME -> "BANK · Nuevo ingreso";
            case EXPENSE -> "BANK · Nuevo gasto";
            case TRANSFER -> "BANK · Transferencia";
            case ENVELOPE_TRANSFER -> "BANK · Transferencia entre sobres";
            case REALLOCATE -> "BANK · Reasignación";
            case ALLOCATE -> "BANK · Nuevo apartado";
            case RELEASE -> "BANK · Liberación";
            case OPENING_BALANCE -> "BANK · Saldo inicial";
            case REVERSAL -> "BANK · Movimiento revertido";
          };
      String money = String.format(Locale.US, "$%,.2f MXN", movement.amount());
      String message = correction
          ? movement.actor() + " corrigió un movimiento por " + money
          : movement.actor() + " " + action + " " + money;
      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("eventType", event);
      metadata.put("movementId", movement.id());
      metadata.put("householdId", movement.householdId());
      metadata.put("movementType", movement.type().name());
      if (movement.periodId() != null) metadata.put("periodId", movement.periodId());
      if (movement.reversalOf() != null) metadata.put("reversalOf", movement.reversalOf());
      schedule(new SendHouseholdNotificationCommand(
          movement.householdId(),
          "bank-movement-" + movement.id(),
          title,
          message,
          metadata));
    } catch (Exception error) {
      log.warn("No se pudo preparar aviso del movimiento {}", movement.id(), error);
    }
  }

  private void schedule(SendHouseholdNotificationCommand notification) {
    // In-memory unit tests don't have a Spring-managed Mongo transaction.
    if (!TransactionSynchronizationManager.isSynchronizationActive()
        || !TransactionSynchronizationManager.isActualTransactionActive()) {
      log.debug("Sin transacción activa; se omite aviso {}", notification.notificationId());
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          notifications.send(notification).subscribe(
              ignored -> {},
              error -> log.warn("Error enviando aviso {}", notification.notificationId(), error),
              () -> log.debug("Aviso procesado por BANK {}", notification.notificationId()));
        } catch (Exception error) {
          log.warn("No se pudo iniciar aviso {}", notification.notificationId(), error);
        }
      }
    });
  }
}

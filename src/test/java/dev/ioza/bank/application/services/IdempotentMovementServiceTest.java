package dev.ioza.bank.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.ioza.bank.infrastructure.adapters.out.mongo.JsonCommandFingerprint;
import dev.ioza.bank.application.commands.Allocation;
import dev.ioza.bank.application.ports.out.IdempotencyStore;
import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.ForbiddenException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.IdempotencyRecord;
import dev.ioza.bank.domain.model.Movement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IdempotentMovementServiceTest {
  final Map<String, IdempotencyRecord> data = new HashMap<>();
  final AtomicBoolean authorized = new AtomicBoolean(true);
  final IdempotencyStore store =
      new IdempotencyStore() {
        public Optional<IdempotencyRecord> find(String id) {
          return Optional.ofNullable(data.get(id));
        }

        public void insert(IdempotencyRecord record) {
          if (data.putIfAbsent(record.id(), record) != null)
            throw new ConflictException("Duplicado");
        }
      };
  final AccessService access =
      new AccessService(null, null, () -> "olda") {
        @Override
        public void requireEditor(String h) {
          if (!authorized.get()) throw new ForbiddenException("Sin acceso");
        }
      };
  final IdempotentMovementService service =
      new IdempotentMovementService(
          store, new JsonCommandFingerprint(JsonMapper.builder().build()), access);
  final Allocation command = new Allocation("q", "e", new BigDecimal("50.00"), "Apartado");
  final Movement response =
      new Movement(
          "m",
          "h",
          "q",
          MovementType.ALLOCATE,
          MovementStatus.POSTED,
          new BigDecimal("50.00"),
          "Apartado",
          "e",
          null,
          "olda",
          Instant.EPOCH,
          null,
          List.of(),
          List.of(),
          List.of());

  @Test
  void repeatedRequestExecutesOnlyOnce() {
    AtomicInteger calls = new AtomicInteger();
    assertEquals(
        response,
        service.execute(
            "h",
            "key",
            "allocate",
            command,
            () -> {
              calls.incrementAndGet();
              return response;
            }));
    assertEquals(
        response,
        service.execute(
            "h",
            "key",
            "allocate",
            command,
            () -> {
              throw new AssertionError("Duplicado");
            }));
    assertEquals(1, calls.get());
  }

  @Test
  void keyIsScopedToHouseholdAndOperationAndBodyAreChecked() {
    service.execute("h", "key", "allocate", command, () -> response);
    assertThrows(
        ConflictException.class,
        () -> service.execute("h", "key", "release", command, () -> response));
    assertThrows(
        ConflictException.class,
        () ->
            service.execute(
                "h",
                "key",
                "allocate",
                new Allocation("q", "e", new BigDecimal("51.00"), "Apartado"),
                () -> response));
    service.execute("other", "key", "allocate", command, () -> response);
    assertEquals(2, data.size());
  }

  @Test
  void replayStillRequiresAuthorizationAndKeyValidation() {
    service.execute("h", "key", "allocate", command, () -> response);
    authorized.set(false);
    assertThrows(
        ForbiddenException.class,
        () -> service.execute("h", "key", "allocate", command, () -> response));
    authorized.set(true);
    assertThrows(
        ValidationException.class,
        () -> service.execute("h", " ", "allocate", command, () -> response));
    assertThrows(
        ValidationException.class,
        () -> service.execute("h", "x".repeat(129), "allocate", command, () -> response));
  }

  @Test
  void failedActionDoesNotReserveKey() {
    assertThrows(
        IllegalStateException.class,
        () ->
            service.execute(
                "h",
                "key",
                "allocate",
                command,
                () -> {
                  throw new IllegalStateException("Fallo");
                }));
    assertEquals(0, data.size());
    service.execute("h", "key", "allocate", command, () -> response);
    assertEquals(1, data.size());
  }
}

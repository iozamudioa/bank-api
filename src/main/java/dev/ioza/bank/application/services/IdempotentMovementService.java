package dev.ioza.bank.application.services;

import dev.ioza.bank.application.ports.in.IdempotentMovements;
import dev.ioza.bank.application.ports.out.CommandFingerprint;
import dev.ioza.bank.application.ports.out.IdempotencyStore;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.IdempotencyRecord;
import dev.ioza.bank.domain.model.Movement;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The idempotency record, movement and balances commit or roll back together. */
@Service
public class IdempotentMovementService implements IdempotentMovements {
  private final IdempotencyStore store;
  private final CommandFingerprint fingerprints;
  private final AccessService access;

  public IdempotentMovementService(
      IdempotencyStore store, CommandFingerprint fingerprints, AccessService access) {
    this.store = store;
    this.fingerprints = fingerprints;
    this.access = access;
  }

  @Transactional
  public Movement execute(
      String householdId, String key, String operation, Object command, Supplier<Movement> action) {
    access.requireEditor(householdId);
    if (key == null || key.isBlank() || key.length() > 128) {
      throw new ValidationException("Idempotency-Key debe contener entre 1 y 128 caracteres");
    }
    String id = digest(householdId + "\0" + key);
    String fingerprint = fingerprints.fingerprint(operation, command);
    IdempotencyRecord previous = store.find(id).orElse(null);
    if (previous != null) {
      if (!previous.fingerprint().equals(fingerprint)) {
        throw new ConflictException("Idempotency-Key ya utilizada con otra operación o contenido");
      }
      return previous.response();
    }
    Movement movement = action.get();
    store.insert(new IdempotencyRecord(id, householdId, fingerprint, movement));
    return movement;
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no disponible", e);
    }
  }
}

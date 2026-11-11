package dev.ioza.bank.application.queries;

import dev.ioza.bank.domain.exceptions.ValidationException;

public record MovementQuery(
    String periodId, String accountId, String envelopeId, int page, int size) {
  public MovementQuery {
    if (page < 0 || size < 1 || size > 100) {
      throw new ValidationException("page debe ser >= 0 y size entre 1 y 100");
    }
  }
}

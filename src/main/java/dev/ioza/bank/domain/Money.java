package dev.ioza.bank.domain;

import dev.ioza.bank.domain.exceptions.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
  public static final BigDecimal ZERO = new BigDecimal("0.00");

  private Money() {}

  public static BigDecimal normalized(BigDecimal value) {
    if (value == null) throw new ValidationException("El importe es obligatorio");
    if (value.scale() > 2)
      throw new ValidationException("El importe debe tener como máximo dos decimales");
    try {
      BigDecimal result = value.setScale(2, RoundingMode.UNNECESSARY);
      if (result.precision() > 34)
        throw new ValidationException("El importe excede la precisión Decimal128");
      return result;
    } catch (ArithmeticException e) {
      throw new ValidationException("El importe debe tener como máximo dos decimales");
    }
  }

  public static BigDecimal positive(BigDecimal value) {
    BigDecimal v = normalized(value);
    if (v.signum() <= 0) throw new ValidationException("El importe debe ser mayor que cero");
    return v;
  }

  public static void nonNegative(BigDecimal amount, String name) {
    if (amount.compareTo(ZERO) < 0) throw new ValidationException("Saldo insuficiente: " + name);
  }
}

package dev.ioza.bank.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.ioza.bank.domain.Money;
import dev.ioza.bank.domain.exceptions.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
  @Test
  void conservaDecimalesExactos() {
    assertEquals(new BigDecimal("50.00"), Money.positive(new BigDecimal("50")));
  }

  @Test
  void rechazaTresDecimales() {
    assertThrows(ValidationException.class, () -> Money.positive(new BigDecimal("1.001")));
  }

  @Test
  void rechazaCero() {
    assertThrows(ValidationException.class, () -> Money.positive(BigDecimal.ZERO));
  }

  @Test
  void rechazaImportesFueraDeDecimal128() {
    assertThrows(ValidationException.class, () -> Money.positive(new BigDecimal("1E+33")));
  }

  @Test
  void rechazaNuloYNegativo() {
    assertThrows(ValidationException.class, () -> Money.positive(null));
    assertThrows(ValidationException.class, () -> Money.positive(new BigDecimal("-0.01")));
  }
}

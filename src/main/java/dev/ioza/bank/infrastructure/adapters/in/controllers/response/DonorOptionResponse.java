package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.application.ports.in.DonorOption;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
    description =
        "DonorOptionResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record DonorOptionResponse(
    @Schema(
            description = "Identificador del sobre del hogar.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "envelope-central",
            accessMode = Schema.AccessMode.READ_ONLY)
        String envelopeId,
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar",
            accessMode = Schema.AccessMode.READ_ONLY)
        String name,
    @Schema(
            description =
                "Saldo persistido. En cuenta es dinero real; en sobre es dinero reservado dentro de"
                    + " su cuenta. Se serializa como número JSON decimal, nunca Extended JSON de"
                    + " MongoDB; el frontend debe usar aritmética decimal.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "number",
            format = "decimal",
            example = "80",
            accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal balance) {
  public static DonorOptionResponse from(DonorOption value) {
    return new DonorOptionResponse(value.envelopeId(), value.name(), value.balance());
  }
}

package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.model.Household;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "HouseholdResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record HouseholdResponse(
    @Schema(
            description = "Identificador opaco; no interpretar ni generar en el frontend.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash",
            accessMode = Schema.AccessMode.READ_ONLY)
        String id,
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar",
            accessMode = Schema.AccessMode.READ_ONLY)
        String name,
    @Schema(
            description = "Moneda del hogar; actualmente MXN.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "MXN",
            accessMode = Schema.AccessMode.READ_ONLY)
        String currency,
    @Schema(
            description = "Zona IANA del hogar; actualmente America/Mexico_City.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "America/Mexico_City",
            accessMode = Schema.AccessMode.READ_ONLY)
        String timezone,
    @Schema(
            description = "Indica si el recurso o membresía está activo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY)
        boolean active) {
  public static HouseholdResponse from(Household value) {
    return new HouseholdResponse(
        value.id(), value.name(), value.currency(), value.timezone(), value.active());
  }
}

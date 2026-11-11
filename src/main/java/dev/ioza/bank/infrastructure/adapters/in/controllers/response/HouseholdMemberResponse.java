package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import dev.ioza.bank.domain.enums.MemberRole;
import dev.ioza.bank.domain.model.HouseholdMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "HouseholdMemberResponse: respuesta REST independiente de MongoDB; importes numéricos y"
            + " campos auditados.")
public record HouseholdMemberResponse(
    @Schema(
            description = "Identificador opaco; no interpretar ni generar en el frontend.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "account-cash",
            accessMode = Schema.AccessMode.READ_ONLY)
        String id,
    @Schema(
            description = "Identificador del hogar al que pertenece el recurso.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "household-demo",
            accessMode = Schema.AccessMode.READ_ONLY)
        String householdId,
    @Schema(
            description = "Identidad HTTP Basic. Agregar una membresía no registra credenciales.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "olda",
            accessMode = Schema.AccessMode.READ_ONLY)
        String username,
    @Schema(
            description = "Nombre visible del integrante.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Olda",
            accessMode = Schema.AccessMode.READ_ONLY)
        String displayName,
    @Schema(
            description =
                "OWNER, EDITOR o VIEWER en respuestas. Al agregar sólo EDITOR/VIEWER; omitido usa"
                    + " EDITOR.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "EDITOR",
            accessMode = Schema.AccessMode.READ_ONLY)
        MemberRole role,
    @Schema(
            description = "Indica si el recurso o membresía está activo.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY)
        boolean active) {
  public static HouseholdMemberResponse from(HouseholdMember value) {
    return new HouseholdMemberResponse(
        value.id(),
        value.householdId(),
        value.username(),
        value.displayName(),
        value.role(),
        value.active());
  }
}

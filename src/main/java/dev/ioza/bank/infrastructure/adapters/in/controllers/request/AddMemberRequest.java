package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.AddMember;
import dev.ioza.bank.domain.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description =
        "AddMemberRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example = "{\"username\":\"cecy\",\"displayName\":\"Cecy\",\"role\":\"EDITOR\"}")
public record AddMemberRequest(
    @Schema(
            description = "Identidad HTTP Basic. Agregar una membresía no registra credenciales.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "olda")
        @NotBlank
        String username,
    @Schema(
            description = "Nombre visible del integrante.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Olda")
        @NotBlank
        String displayName,
    @Schema(
            description =
                "OWNER, EDITOR o VIEWER en respuestas. Al agregar sólo EDITOR/VIEWER; omitido usa"
                    + " EDITOR.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
            example = "EDITOR",
            allowableValues = {"EDITOR", "VIEWER"})
        MemberRole role) {
  public AddMember toCommand() {
    return new AddMember(username, displayName, role);
  }
}

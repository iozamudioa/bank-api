package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.CreateHousehold;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    description =
        "CreateHouseholdRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example = "{\"name\":\"Familia\",\"displayName\":\"Olda\"}")
public record CreateHouseholdRequest(
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar")
        @NotBlank
        String name,
    @Schema(
            description = "Nombre visible del integrante.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Olda")
        @NotBlank
        String displayName) {
  public CreateHousehold toCommand() {
    return new CreateHousehold(name, displayName);
  }
}

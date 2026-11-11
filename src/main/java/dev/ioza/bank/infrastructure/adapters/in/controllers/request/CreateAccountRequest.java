package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.CreateAccount;
import dev.ioza.bank.domain.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(
    description =
        "CreateAccountRequest: entrada REST; validaciones Jakarta y reglas adicionales en la"
            + " operación.",
    example = "{\"name\":\"Efectivo familiar\",\"type\":\"PHYSICAL\"}")
public record CreateAccountRequest(
    @Schema(
            description = "Nombre visible.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Efectivo familiar")
        @NotBlank
        String name,
    @Schema(
            description = "Tipo del recurso según el enum del contrato.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "PHYSICAL")
        @NotNull
        AccountType type) {
  public CreateAccount toCommand() {
    return new CreateAccount(name, type);
  }
}

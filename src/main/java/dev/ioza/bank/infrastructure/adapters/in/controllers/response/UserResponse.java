package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "UserResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record UserResponse(
    @Schema(
            description = "Identidad HTTP Basic. Agregar una membresía no registra credenciales.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "olda",
            accessMode = Schema.AccessMode.READ_ONLY)
        String username) {}

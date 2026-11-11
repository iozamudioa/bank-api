package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "ErrorResponse: respuesta REST independiente de MongoDB; importes numéricos y campos"
            + " auditados.")
public record ErrorResponse(
    @Schema(
            description = "Código HTTP simbólico; no es un código específico de regla financiera.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "BAD_REQUEST",
            accessMode = Schema.AccessMode.READ_ONLY)
        String error,
    @Schema(
            description = "Mensaje humano. No analizarlo como identificador estable de error.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Saldo insuficiente: sobre Central",
            accessMode = Schema.AccessMode.READ_ONLY)
        String message) {}

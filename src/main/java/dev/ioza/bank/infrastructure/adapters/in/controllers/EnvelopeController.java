package dev.ioza.bank.infrastructure.adapters.in.controllers;

import dev.ioza.bank.application.ports.in.Setup;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.CreateEnvelopeRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.EnvelopeResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/envelopes")
public class EnvelopeController {
  private final Setup setup;

  public EnvelopeController(Setup setup) {
    this.setup = setup;
  }

  @Operation(
      operationId = "createEnvelope",
      tags = "Envelopes",
      summary = "Crear sobre",
      description =
          "OWNER o EDITOR. Cuenta activa del mismo hogar; reserva inicial cero. Físico o digital se"
              + " deriva de la cuenta, no del tipo del sobre. habitualAmount opcional crea o amplia"
              + " la plantilla en la misma transaccion, conservando otras lineas e ingreso"
              + " esperado. No modifica saldos ni snapshots. Idempotency-Key opcional para evitar"
              + " altas duplicadas.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EnvelopeResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"envelope-central\",\"householdId\":\"household-demo\",\"name\":\"Central"
                                + " de Abastos\",\"accountId\":\"account-cash\",\"type\":\"SPENDING\",\"category\":\"Alimentación\",\"balance\":0,\"version\":0,\"active\":true,\"habitualAmount\":null}"))),
    @ApiResponse(
        responseCode = "400",
        description =
            "Request, importe o filtros inválidos; saldo insuficiente también devuelve 400.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"BAD_REQUEST\",\"message\":\"Solicitud inválida o"
                                + " incompleta\"}"))),
    @ApiResponse(
        responseCode = "401",
        description = "Credenciales HTTP Basic ausentes o inválidas.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"UNAUTHORIZED\",\"message\":\"Autenticación HTTP Basic"
                                + " requerida o credenciales inválidas\"}"))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin membresía activa o permisos de escritura/OWNER.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"FORBIDDEN\",\"message\":\"No perteneces a este"
                                + " hogar\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "Hogar o recurso no encontrado para este hogar.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value = "{\"error\":\"NOT_FOUND\",\"message\":\"Hogar no encontrado\"}"))),
    @ApiResponse(
        responseCode = "409",
        description =
            "Conflicto de negocio, clave reutilizada con datos distintos, versión o transacción"
                + " concurrente.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"CONFLICT\",\"message\":\"Conflicto de operación;"
                                + " reintenta con la misma Idempotency-Key. Si cambiaste el"
                                + " contenido usa otra clave.\"}"))),
    @ApiResponse(
        responseCode = "503",
        description =
            "Persistencia no disponible o confirmación incierta; en movimientos repetir misma clave"
                + " y datos.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"SERVICE_UNAVAILABLE\",\"message\":\"Persistencia no"
                                + " disponible o confirmación incierta; reintenta con la misma"
                                + " Idempotency-Key\"}"))),
    @ApiResponse(
        responseCode = "500",
        description =
            "Error interno inesperado; no crear otra clave para repetir una operación financiera"
                + " incierta.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Error interno; si"
                                + " ocurrió durante una operación financiera reintenta con la misma"
                                + " Idempotency-Key\"}")))
  })
  @PostMapping
  public EnvelopeResponse create(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description =
                  "Opcional por compatibilidad; enviar para reintentos seguros del alta. 1 a 128"
                      + " caracteres. Mismo hogar/clave/cuerpo devuelve la respuesta original;"
                      + " cuerpo distinto produce 409.")
          @RequestHeader(value = "Idempotency-Key", required = false)
          String key,
      @Valid @RequestBody CreateEnvelopeRequest req) {
    return EnvelopeResponse.from(setup.createEnvelopeWithPlan(householdId, req.toCommand(), key));
  }

  @Operation(
      operationId = "listEnvelopes",
      tags = "Envelopes",
      summary = "Listar sobres",
      description =
          "Requiere membresía activa. Incluye accountId y balance acumulado; no hay endpoint"
              + " individual de sobre. habitualAmount deriva de la plantilla sin requerir"
              + " quincena.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = EnvelopeResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"envelope-central\",\"householdId\":\"household-demo\",\"name\":\"Central"
                                + " de Abastos\",\"accountId\":\"account-cash\",\"type\":\"SPENDING\",\"category\":\"Alimentación\",\"balance\":80,\"version\":0,\"active\":true,\"habitualAmount\":null}]"))),
    @ApiResponse(
        responseCode = "401",
        description = "Credenciales HTTP Basic ausentes o inválidas.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"UNAUTHORIZED\",\"message\":\"Autenticación HTTP Basic"
                                + " requerida o credenciales inválidas\"}"))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin membresía activa o permisos de escritura/OWNER.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"FORBIDDEN\",\"message\":\"No perteneces a este"
                                + " hogar\"}"))),
    @ApiResponse(
        responseCode = "404",
        description = "Hogar o recurso no encontrado para este hogar.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value = "{\"error\":\"NOT_FOUND\",\"message\":\"Hogar no encontrado\"}"))),
    @ApiResponse(
        responseCode = "503",
        description =
            "Persistencia no disponible o confirmación incierta; en movimientos repetir misma clave"
                + " y datos.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"SERVICE_UNAVAILABLE\",\"message\":\"Persistencia no"
                                + " disponible o confirmación incierta; reintenta con la misma"
                                + " Idempotency-Key\"}"))),
    @ApiResponse(
        responseCode = "500",
        description =
            "Error interno inesperado; no crear otra clave para repetir una operación financiera"
                + " incierta.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Error interno; si"
                                + " ocurrió durante una operación financiera reintenta con la misma"
                                + " Idempotency-Key\"}")))
  })
  @GetMapping
  public List<EnvelopeResponse> list(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return setup.envelopesWithPlans(householdId).stream().map(EnvelopeResponse::from).toList();
  }
}

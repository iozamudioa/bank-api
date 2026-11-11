package dev.ioza.bank.infrastructure.adapters.in.controllers;

import dev.ioza.bank.application.ports.in.Setup;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.CreateAccountRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.CreateIncomeSourceRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.AccountResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.IncomeSourceResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class AccountController {
  private final Setup setup;

  public AccountController(Setup setup) {
    this.setup = setup;
  }

  @Operation(
      operationId = "createAccount",
      tags = "Accounts",
      summary = "Crear cuenta",
      description =
          "OWNER o EDITOR. Saldo inicial cero; registra dinero real mediante movimientos.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"account-cash\",\"householdId\":\"household-demo\",\"name\":\"Efectivo"
                                + " familiar\",\"type\":\"PHYSICAL\",\"balance\":0,\"version\":0,\"active\":true}"))),
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
  @PostMapping("/accounts")
  public AccountResponse create(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody CreateAccountRequest req) {
    return AccountResponse.from(setup.createAccount(householdId, req.toCommand()));
  }

  @Operation(
      operationId = "listAccounts",
      tags = "Accounts",
      summary = "Listar cuentas",
      description = "Requiere membresía activa. Incluye balance persistido y version técnica.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = AccountResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"account-cash\",\"householdId\":\"household-demo\",\"name\":\"Efectivo"
                                + " familiar\",\"type\":\"PHYSICAL\",\"balance\":1080,\"version\":0,\"active\":true}]"))),
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
  @GetMapping("/accounts")
  public List<AccountResponse> list(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return setup.accounts(householdId).stream().map(AccountResponse::from).toList();
  }

  @Operation(
      operationId = "createIncomeSource",
      tags = "Income Sources",
      summary = "Crear fuente de ingreso",
      description = "OWNER o EDITOR. Catálogo de ingreso esperado; no deposita dinero.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = IncomeSourceResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"income-payroll\",\"householdId\":\"household-demo\",\"name\":\"Nómina\",\"type\":\"PAYROLL\",\"defaultAccountId\":\"account-digital\",\"expectedAmount\":21000,\"active\":true}"))),
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
  @PostMapping("/income-sources")
  public IncomeSourceResponse source(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody CreateIncomeSourceRequest req) {
    return IncomeSourceResponse.from(setup.createIncomeSource(householdId, req.toCommand()));
  }

  @Operation(
      operationId = "listIncomeSources",
      tags = "Income Sources",
      summary = "Listar fuentes de ingreso",
      description = "Requiere membresía activa; no equivale al historial de depósitos.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = IncomeSourceResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"income-payroll\",\"householdId\":\"household-demo\",\"name\":\"Nómina\",\"type\":\"PAYROLL\",\"defaultAccountId\":\"account-digital\",\"expectedAmount\":21000,\"active\":true}]"))),
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
  @GetMapping("/income-sources")
  public List<IncomeSourceResponse> sources(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return setup.incomeSources(householdId).stream().map(IncomeSourceResponse::from).toList();
  }
}

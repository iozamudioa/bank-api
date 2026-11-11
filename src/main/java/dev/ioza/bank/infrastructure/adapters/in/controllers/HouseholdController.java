package dev.ioza.bank.infrastructure.adapters.in.controllers;

import dev.ioza.bank.application.ports.in.Setup;
import dev.ioza.bank.application.ports.out.CurrentUser;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.AddMemberRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.CreateHouseholdRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.HouseholdMemberResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.HouseholdResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.UserResponse;
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
@RequestMapping("/api/v1")
public class HouseholdController {
  private final Setup setup;
  private final CurrentUser user;

  public HouseholdController(Setup setup, CurrentUser user) {
    this.setup = setup;
    this.user = user;
  }

  @Operation(
      operationId = "getCurrentUser",
      tags = "Households",
      summary = "Consultar identidad autenticada",
      description =
          "Devuelve el username de HTTP Basic. No existe login con token ni registro de usuarios.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(value = "{\"username\":\"olda\"}"))),
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
  @GetMapping("/me")
  public UserResponse me() {
    return new UserResponse(user.username());
  }

  @Operation(
      operationId = "createHousehold",
      tags = "Households",
      summary = "Crear hogar",
      description = "Crea el hogar MXN y su membresía OWNER de forma transaccional.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HouseholdResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"household-demo\",\"name\":\"Familia\",\"currency\":\"MXN\",\"timezone\":\"America/Mexico_City\",\"active\":true}"))),
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
  @PostMapping("/households")
  public HouseholdResponse create(@Valid @RequestBody CreateHouseholdRequest req) {
    return HouseholdResponse.from(setup.createHousehold(req.toCommand()));
  }

  @Operation(
      operationId = "listHouseholds",
      tags = "Households",
      summary = "Listar mis hogares",
      description = "Sólo devuelve hogares con membresía activa del usuario.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = HouseholdResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"household-demo\",\"name\":\"Familia\",\"currency\":\"MXN\",\"timezone\":\"America/Mexico_City\",\"active\":true}]"))),
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
  @GetMapping("/households")
  public List<HouseholdResponse> all() {
    return setup.households().stream().map(HouseholdResponse::from).toList();
  }

  @Operation(
      operationId = "getHousehold",
      tags = "Households",
      summary = "Consultar hogar",
      description = "Requiere membresía activa.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HouseholdResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"household-demo\",\"name\":\"Familia\",\"currency\":\"MXN\",\"timezone\":\"America/Mexico_City\",\"active\":true}"))),
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
  @GetMapping("/households/{householdId}")
  public HouseholdResponse get(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return HouseholdResponse.from(setup.household(householdId));
  }

  @Operation(
      operationId = "listHouseholdMembers",
      tags = "Members",
      summary = "Listar integrantes",
      description = "Requiere membresía activa del hogar.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array =
                    @ArraySchema(schema = @Schema(implementation = HouseholdMemberResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"account-cash\",\"householdId\":\"household-demo\",\"username\":\"olda\",\"displayName\":\"Olda\",\"role\":\"EDITOR\",\"active\":true}]"))),
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
  @GetMapping("/households/{householdId}/members")
  public List<HouseholdMemberResponse> members(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return setup.members(householdId).stream().map(HouseholdMemberResponse::from).toList();
  }

  @Operation(
      operationId = "addHouseholdMember",
      tags = "Members",
      summary = "Agregar integrante",
      description =
          "Sólo OWNER. Asocia un username existente o futuro; no crea credenciales ni envía"
              + " invitaciones. role omitido usa EDITOR; OWNER no permitido.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = HouseholdMemberResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"account-cash\",\"householdId\":\"household-demo\",\"username\":\"olda\",\"displayName\":\"Olda\",\"role\":\"EDITOR\",\"active\":true}"))),
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
  @PostMapping("/households/{householdId}/members")
  public HouseholdMemberResponse addMember(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody AddMemberRequest req) {
    return HouseholdMemberResponse.from(setup.addMember(householdId, req.toCommand()));
  }
}

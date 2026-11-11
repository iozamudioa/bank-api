package dev.ioza.bank.infrastructure.adapters.in.controllers;

import dev.ioza.bank.application.ports.in.Finance;
import dev.ioza.bank.application.ports.in.IdempotentMovements;
import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.AccountExpenseRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.AllocationRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.CorrectionRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.EnvelopeTransferRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.ExpenseRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.IncomeRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.OpeningBalanceRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.ReallocationRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.ReverseRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.TransferRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.AccountExpensePreviewResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.CorrectionPreviewResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ExpensePreviewResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.MovementResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/movements")
public class MovementController {
  private final Finance finance;
  private final IdempotentMovements idempotency;

  public MovementController(Finance finance, IdempotentMovements idempotency) {
    this.finance = finance;
    this.idempotency = idempotency;
  }

  @Operation(
      operationId = "listMovements",
      tags = "Movements",
      summary = "Consultar historial paginado",
      description =
          "Requiere membresía activa. Filtros AND en MongoDB antes de paginar, por periodo, cuenta"
              + " y sobre. Lista JSON sin total; orden occurredAt e id descendente.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MovementResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"movement-expense\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"EXPENSE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[{\"envelopeId\":\"envelope-central\",\"amount\":80},{\"envelopeId\":\"envelope-sams\",\"amount\":70}],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":1080,\"change\":-150,\"after\":930}],\"envelopeEffects\":[{\"entityId\":\"envelope-central\",\"before\":80,\"change\":-80,\"after\":0},{\"entityId\":\"envelope-sams\",\"before\":1000,\"change\":-70,\"after\":930}]}]"))),
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
  public List<MovementResponse> all(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(description = "Filtro opcional combinado con AND; identificador periodId.")
          @RequestParam(required = false)
          String periodId,
      @Parameter(description = "Filtro opcional combinado con AND; identificador accountId.")
          @RequestParam(required = false)
          String accountId,
      @Parameter(description = "Filtro opcional combinado con AND; identificador envelopeId.")
          @RequestParam(required = false)
          String envelopeId,
      @Parameter(
              description = "Página base cero; sin total ni cursor.",
              schema = @Schema(minimum = "0", defaultValue = "0"))
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(
              description = "Tamaño de página entre 1 y 100.",
              schema = @Schema(minimum = "1", maximum = "100", defaultValue = "100"))
          @RequestParam(defaultValue = "100")
          int size) {
    return finance
        .movements(householdId, new MovementQuery(periodId, accountId, envelopeId, page, size))
        .stream()
        .map(MovementResponse::from)
        .toList();
  }

  @Operation(
      operationId = "getMovement",
      tags = "Movements",
      summary = "Consultar movimiento",
      description =
          "Requiere membresía activa. Incluye efectos antes/cambio/después y fuentes de gasto.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-expense\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"EXPENSE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[{\"envelopeId\":\"envelope-central\",\"amount\":80},{\"envelopeId\":\"envelope-sams\",\"amount\":70}],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":1080,\"change\":-150,\"after\":930}],\"envelopeEffects\":[{\"entityId\":\"envelope-central\",\"before\":80,\"change\":-80,\"after\":0},{\"entityId\":\"envelope-sams\",\"before\":1000,\"change\":-70,\"after\":930}]}"))),
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
  @GetMapping("/{id}")
  public MovementResponse get(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Identificador del movimiento del hogar.",
              required = true,
              example = "movement-expense")
          @PathVariable
          String id) {
    return MovementResponse.from(finance.movement(householdId, id));
  }

  @Operation(
      operationId = "registerOpeningBalance",
      tags = "Balances",
      summary = "Registrar saldo inicial real",
      description =
          "OWNER o EDITOR. Una sola vez en la historia de la cuenta, incluso después de revertir."
              + " Cuenta vacía sin reservas. No requiere quincena; periodId se ignora."
              + " Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-open\",\"householdId\":\"household-demo\",\"periodId\":null,\"type\":\"OPENING_BALANCE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":null,\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":0,\"change\":150,\"after\":150}],\"envelopeEffects\":[]}"))),
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
  @PostMapping("/opening-balances")
  public MovementResponse open(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description =
                  "Clave opaca de 1 a 128 caracteres por hogar y operación lógica. Repetir misma"
                      + " clave y command devuelve respuesta original; contenido distinto produce"
                      + " 409. Carrera puede requerir reintento con misma clave.",
              required = true,
              example = "purchase-carne-001",
              schema = @Schema(minLength = 1, maxLength = 128))
          @RequestHeader("Idempotency-Key")
          String key,
      @Valid @RequestBody OpeningBalanceRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "opening",
            req.toCommand(),
            () -> finance.opening(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "registerIncome",
      tags = "Movements",
      summary = "Registrar ingreso real",
      description =
          "OWNER o EDITOR y quincena OPEN. Aumenta sólo el saldo de la cuenta activa del hogar;"
              + " fuente opcional debe estar activa y ser del hogar. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-income\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"INCOME\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":null,\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":0,\"change\":150,\"after\":150}],\"envelopeEffects\":[]}"))),
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
  @PostMapping("/incomes")
  public MovementResponse income(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody IncomeRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "income",
            req.toCommand(),
            () -> finance.income(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "allocateMoney",
      tags = "Movements",
      summary = "Apartar dinero en un sobre",
      description =
          "OWNER o EDITOR y quincena OPEN. Aumenta reserva del sobre sin descontar saldo real de"
              + " cuenta; exige dinero libre suficiente. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-allocate\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"ALLOCATE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":1080,\"change\":0,\"after\":1080}],\"envelopeEffects\":[{\"entityId\":\"envelope-central\",\"before\":0,\"change\":150,\"after\":150}]}"))),
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
  @PostMapping("/allocations")
  public MovementResponse allocate(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody AllocationRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "allocate",
            req.toCommand(),
            () -> finance.allocate(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "releaseReserve",
      tags = "Movements",
      summary = "Liberar reserva",
      description =
          "OWNER o EDITOR y quincena OPEN. Disminuye reserva y aumenta dinero libre, sin cambiar"
              + " total de cuenta. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-release\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"RELEASE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":1080,\"change\":0,\"after\":1080}],\"envelopeEffects\":[{\"entityId\":\"envelope-central\",\"before\":150,\"change\":-150,\"after\":0}]}"))),
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
  @PostMapping("/releases")
  public MovementResponse release(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody AllocationRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "release",
            req.toCommand(),
            () -> finance.release(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "reallocateEnvelopes",
      tags = "Movements",
      summary = "Reasignar entre sobres de la misma cuenta",
      description =
          "OWNER o EDITOR y quincena OPEN. Sobres diferentes de la misma cuenta; salida e ingreso"
              + " iguales, sin modificar total de cuenta. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-reallocate\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"REALLOCATE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":1080,\"change\":0,\"after\":1080}],\"envelopeEffects\":[{\"entityId\":\"envelope-sams\",\"before\":1000,\"change\":-150,\"after\":850},{\"entityId\":\"envelope-central\",\"before\":80,\"change\":150,\"after\":230}]}"))),
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
  @PostMapping("/reallocations")
  public MovementResponse reallocate(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ReallocationRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "reallocate",
            req.toCommand(),
            () -> finance.reallocate(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "transferAccounts",
      tags = "Movements",
      summary = "Transferir dinero libre entre cuentas",
      description =
          "OWNER o EDITOR y quincena OPEN. Cuentas diferentes del mismo hogar; sólo dinero no"
              + " reservado. No es gasto y conserva patrimonio total. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-transfer\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"TRANSFER\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":null,\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-digital\",\"before\":1080,\"change\":-150,\"after\":930},{\"entityId\":\"account-cash\",\"before\":0,\"change\":150,\"after\":150}],\"envelopeEffects\":[]}"))),
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
  @PostMapping("/transfers")
  public MovementResponse transfer(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody TransferRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "transfer",
            req.toCommand(),
            () -> finance.transfer(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "transferEnvelopes",
      tags = "Movements",
      summary = "Transferir reserva entre cuentas y sobres",
      description =
          "OWNER o EDITOR y quincena OPEN. Sobres de cuentas diferentes del hogar; mueve dinero"
              + " real y reserva por el mismo importe. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-envelopeTransfer\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"ENVELOPE_TRANSFER\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-digital\",\"before\":1080,\"change\":-150,\"after\":930},{\"entityId\":\"account-cash\",\"before\":0,\"change\":150,\"after\":150}],\"envelopeEffects\":[{\"entityId\":\"envelope-sams\",\"before\":1000,\"change\":-150,\"after\":850},{\"entityId\":\"envelope-central\",\"before\":0,\"change\":150,\"after\":150}]}"))),
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
  @PostMapping("/envelope-transfers")
  public MovementResponse envelopeTransfer(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody EnvelopeTransferRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "transferEnvelope",
            req.toCommand(),
            () -> finance.transferEnvelope(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "previewExpense",
      tags = "Expenses",
      summary = "Previsualizar gasto y faltante",
      description =
          "Membresía activa, incluso VIEWER. Sólo lectura; no valida que la quincena esté OPEN ni"
              + " garantiza fondos futuros. Donantes activos con saldo positivo en la misma cuenta;"
              + " pueden necesitar combinarse. fundingSources no se valida financieramente aquí. No"
              + " requiere Idempotency-Key.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExpensePreviewResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"requestedAmount\":150,\"targetBalance\":80,\"shortfall\":70,\"eligibleDonors\":[{\"envelopeId\":\"envelope-sams\",\"name\":\"Sam's\",\"balance\":1000}]}"))),
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
  @PostMapping("/expenses/preview")
  public ExpensePreviewResponse preview(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody ExpenseRequest req) {
    return ExpensePreviewResponse.from(finance.previewExpense(householdId, req.toCommand()));
  }

  @Operation(
      operationId = "registerExpense",
      tags = "Expenses",
      summary = "Confirmar gasto simple o financiado",
      description =
          "OWNER o EDITOR y quincena OPEN. Sin fuentes, consume sólo el sobre principal. Con"
              + " fuentes, hay que enviar TODA la financiación: 80 Central + 70 Sam's para gastar"
              + " 150. Fuentes distintas y activas, importes positivos, misma cuenta, suma exacta y"
              + " saldos suficientes. Disminuye cuenta una vez y persiste un único gasto. Revalida"
              + " dentro de la transacción. Idempotency-Key obligatoria; 409 concurrente o 503"
              + " incierto se reintentan con misma clave y datos. Saldo insuficiente devuelve 400.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-expense\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"EXPENSE\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":null,\"funding\":[{\"envelopeId\":\"envelope-central\",\"amount\":80},{\"envelopeId\":\"envelope-sams\",\"amount\":70}],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":1080,\"change\":-150,\"after\":930}],\"envelopeEffects\":[{\"entityId\":\"envelope-central\",\"before\":80,\"change\":-80,\"after\":0},{\"entityId\":\"envelope-sams\",\"before\":1000,\"change\":-70,\"after\":930}]}"))),
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
  @PostMapping("/expenses")
  public MovementResponse expense(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ExpenseRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "expense",
            req.toCommand(),
            () -> finance.expense(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "reverseMovement",
      tags = "Movements",
      summary = "Revertir un movimiento",
      description =
          "OWNER o EDITOR y quincena OPEN de destino. Aplica efectos inversos contra saldos"
              + " actuales y marca original REVERSED atómicamente. No admite reversión de una"
              + " REVERSAL ni segunda reversión. Idempotency-Key obligatoria; no hay ajuste"
              + " arbitrario de saldo.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"movement-reverse\",\"householdId\":\"household-demo\",\"periodId\":\"period-q1\",\"type\":\"REVERSAL\",\"status\":\"POSTED\",\"amount\":150,\"description\":\"Carne\",\"targetEnvelopeId\":\"envelope-central\",\"incomeSourceId\":null,\"actor\":\"olda\",\"occurredAt\":\"2026-11-15T18:00:00Z\",\"reversalOf\":\"movement-expense\",\"funding\":[],\"accountEffects\":[{\"entityId\":\"account-cash\",\"before\":930,\"change\":150,\"after\":1080}],\"envelopeEffects\":[{\"entityId\":\"envelope-central\",\"before\":0,\"change\":80,\"after\":80},{\"entityId\":\"envelope-sams\",\"before\":930,\"change\":70,\"after\":1000}]}"))),
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
  @PostMapping("/{id}/reverse")
  public MovementResponse reverse(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Identificador del movimiento del hogar.",
              required = true,
              example = "movement-expense")
          @PathVariable
          String id,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ReverseRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "reverse:" + id,
            req.toCommand(),
            () -> finance.reverse(householdId, id, req.toCommand())));
  }

  @Operation(
      operationId = "registerAccountExpense",
      tags = "Movements",
      summary = "Registrar gasto directo de cuenta",
      description =
          "OWNER o EDITOR. Periodo OPEN y cuenta activa del hogar. Consume solo saldo libre, sin"
              + " alterar reservas. Idempotency-Key obligatoria.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operacion exitosa.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Solicitud o saldo neto invalido.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Autenticacion requerida.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin permiso de escritura.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso activo del hogar no encontrado.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "Periodo cerrado, original no corregible, clave reutilizada o conflicto concurrente.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description =
            "Persistencia no disponible; en confirmaciones reintentar con misma clave y datos.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/account-expenses")
  public MovementResponse accountExpense(
      @Parameter(
              description = "Hogar con membresia activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody AccountExpenseRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "account-expense",
            req.toCommand(),
            () -> finance.accountExpense(householdId, req.toCommand())));
  }

  @Operation(
      operationId = "previewAccountExpense",
      tags = "Movements",
      summary = "Previsualizar gasto directo de cuenta",
      description =
          "OWNER o EDITOR. Valida periodo OPEN y disponibilidad actual sin escrituras. La"
              + " confirmacion revalida; no requiere Idempotency-Key.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operacion exitosa.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountExpensePreviewResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Solicitud o saldo neto invalido.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Autenticacion requerida.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin permiso de escritura.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso activo del hogar no encontrado.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "Periodo cerrado, original no corregible, clave reutilizada o conflicto concurrente.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description =
            "Persistencia no disponible; en confirmaciones reintentar con misma clave y datos.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/account-expenses/preview")
  public AccountExpensePreviewResponse previewAccountExpense(
      @Parameter(
              description = "Hogar con membresia activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody AccountExpenseRequest req) {
    return AccountExpensePreviewResponse.from(
        finance.previewAccountExpense(householdId, req.toCommand()));
  }

  @Operation(
      operationId = "correctMovement",
      tags = "Movements",
      summary = "Corregir movimiento de forma atomica",
      description =
          "OWNER o EDITOR. Registra compensacion y reemplazo en periodo OPEN, conserva tipo y"
              + " original REVERSED. No admite OPENING_BALANCE, REVERSAL ni originales revertidos."
              + " Valida solo saldos netos finales. Idempotency-Key obligatoria. correction vincula"
              + " original y compensacion; efectos del reemplazo son virtuales para auditoria, no"
              + " saldos intermedios persistidos.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operacion exitosa.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovementResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Solicitud o saldo neto invalido.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Autenticacion requerida.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin permiso de escritura.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso activo del hogar no encontrado.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "Periodo cerrado, original no corregible, clave reutilizada o conflicto concurrente.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description =
            "Persistencia no disponible; en confirmaciones reintentar con misma clave y datos.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/{id}/corrections")
  public MovementResponse correct(
      @Parameter(
              description = "Hogar con membresia activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Movimiento original del hogar.",
              required = true,
              example = "movement-original")
          @PathVariable
          String id,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CorrectionRequest req) {
    return MovementResponse.from(
        idempotency.execute(
            householdId,
            key,
            "correct:" + id,
            req.toCommand(),
            () -> finance.correct(householdId, id, req.toCommand())));
  }

  @Operation(
      operationId = "previewMovementCorrection",
      tags = "Movements",
      summary = "Previsualizar correccion de movimiento",
      description =
          "OWNER o EDITOR. Efectos netos antes/cambio/despues con validaciones de confirmacion; sin"
              + " escrituras. Confirmar revalida. No requiere Idempotency-Key.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operacion exitosa.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CorrectionPreviewResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Solicitud o saldo neto invalido.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Autenticacion requerida.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin permiso de escritura.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Recurso activo del hogar no encontrado.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "Periodo cerrado, original no corregible, clave reutilizada o conflicto concurrente.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description =
            "Persistencia no disponible; en confirmaciones reintentar con misma clave y datos.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/{id}/corrections/preview")
  public CorrectionPreviewResponse previewCorrection(
      @Parameter(
              description = "Hogar con membresia activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Movimiento original del hogar.",
              required = true,
              example = "movement-original")
          @PathVariable
          String id,
      @Valid @RequestBody CorrectionRequest req) {
    return CorrectionPreviewResponse.from(
        finance.previewCorrection(householdId, id, req.toCommand()));
  }
}

package dev.ioza.bank.infrastructure.adapters.in.controllers;

import dev.ioza.bank.application.ports.in.Budget;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.CreatePeriodRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.SaveBudgetRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.UpsertPeriodBudgetLineRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.UpsertTemplateBudgetLineRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.BudgetPeriodResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.BudgetTemplateResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class BudgetController {
  private final Budget budget;

  public BudgetController(Budget budget) {
    this.budget = budget;
  }

  @Operation(
      operationId = "saveBudgetTemplate",
      tags = "Budgets",
      summary = "Guardar presupuesto habitual",
      description =
          "OWNER o EDITOR. Importes no negativos, sobres únicos y activos del hogar. Cambia"
              + " planificación, nunca saldos; no cambia snapshots ya creados.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetTemplateResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"template-demo\",\"householdId\":\"household-demo\",\"expectedIncome\":21000,\"lines\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}"))),
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
  @PutMapping("/budget-template")
  public BudgetTemplateResponse templateSave(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody SaveBudgetRequest req) {
    return BudgetTemplateResponse.from(budget.saveTemplate(householdId, req.toCommand()));
  }

  @Operation(
      operationId = "getBudgetTemplate",
      tags = "Budgets",
      summary = "Consultar presupuesto habitual",
      description = "Requiere membresía activa. 404 si todavía no hay plantilla.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetTemplateResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"template-demo\",\"householdId\":\"household-demo\",\"expectedIncome\":21000,\"lines\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}"))),
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
  @GetMapping("/budget-template")
  public BudgetTemplateResponse template(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return BudgetTemplateResponse.from(budget.template(householdId));
  }

  @Operation(
      operationId = "createBudgetPeriod",
      tags = "Budget Periods",
      summary = "Crear quincena",
      description =
          "OWNER o EDITOR. Copia snapshot de la plantilla; exige fechas ordenadas y sin traslape."
              + " open=true crea OPEN, omitido crea DRAFT. No hay endpoint para abrir un DRAFT. No"
              + " deposita ni reserva dinero.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetPeriodResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"period-q1\",\"householdId\":\"household-demo\",\"startDate\":\"2026-11-15\",\"endDate\":\"2026-11-30\",\"status\":\"OPEN\",\"expectedIncome\":21000,\"lines\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}"))),
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
  @PostMapping("/budget-periods")
  public BudgetPeriodResponse create(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Valid @RequestBody CreatePeriodRequest req) {
    return BudgetPeriodResponse.from(budget.createPeriod(householdId, req.toCommand()));
  }

  @Operation(
      operationId = "listBudgetPeriods",
      tags = "Budget Periods",
      summary = "Listar quincenas",
      description =
          "Requiere membresía activa. Ordenadas por fecha de inicio descendente, de la más reciente"
              + " a la más antigua. Los remanentes están en los sobres, no se reinician al cambiar"
              + " de periodo.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = BudgetPeriodResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            "[{\"id\":\"period-q1\",\"householdId\":\"household-demo\",\"startDate\":\"2026-11-15\",\"endDate\":\"2026-11-30\",\"status\":\"OPEN\",\"expectedIncome\":21000,\"lines\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}]"))),
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
  @GetMapping("/budget-periods")
  public List<BudgetPeriodResponse> all(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return budget.periods(householdId).stream().map(BudgetPeriodResponse::from).toList();
  }

  @Operation(
      operationId = "getBudgetPeriod",
      tags = "Budget Periods",
      summary = "Consultar quincena",
      description = "Devuelve el presupuesto histórico del periodo del hogar.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetPeriodResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"period-q1\",\"householdId\":\"household-demo\",\"startDate\":\"2026-11-15\",\"endDate\":\"2026-11-30\",\"status\":\"OPEN\",\"expectedIncome\":21000,\"lines\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}"))),
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
  @GetMapping("/budget-periods/{periodId}")
  public BudgetPeriodResponse one(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Identificador de la quincena del hogar.",
              required = true,
              example = "period-q1")
          @PathVariable
          String periodId) {
    return BudgetPeriodResponse.from(budget.getPeriod(householdId, periodId));
  }

  @Operation(
      operationId = "closeBudgetPeriod",
      tags = "Budget Periods",
      summary = "Cerrar quincena",
      description =
          "OWNER o EDITOR. Sólo OPEN; pasa a CLOSED sin modificar saldos. No existe reapertura.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetPeriodResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"id\":\"period-q1\",\"householdId\":\"household-demo\",\"startDate\":\"2026-11-15\",\"endDate\":\"2026-11-30\",\"status\":\"CLOSED\",\"expectedIncome\":21000,\"lines\":[{\"envelopeId\":\"envelope-central\",\"amount\":4500},{\"envelopeId\":\"envelope-sams\",\"amount\":1000}]}"))),
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
  @PostMapping("/budget-periods/{periodId}/close")
  public BudgetPeriodResponse close(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Identificador de la quincena del hogar.",
              required = true,
              example = "period-q1")
          @PathVariable
          String periodId) {
    return BudgetPeriodResponse.from(budget.close(householdId, periodId));
  }

  @Operation(
      operationId = "upsertPeriodBudgetLine",
      tags = "Budget Periods",
      summary = "Guardar lo planeado para un sobre",
      description =
          "OWNER o EDITOR. Inserta o reemplaza una linea exclusivamente en una quincena OPEN. Sobre"
              + " y cuenta activos del hogar. Importe no negativo, cero permitido. No modifica"
              + " plantilla, otros periodos, saldos ni movimientos. PUT idempotente; no requiere"
              + " Idempotency-Key. Ante conflicto concurrente, consultar el periodo y reintentar.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Quincena actualizada.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetPeriodResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Importe o sobre/cuenta no disponibles.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Autenticacion requerida.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Sin membresia activa o permiso de escritura.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Periodo, sobre o cuenta no encontrados para el hogar.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Quincena no abierta o escritura concurrente.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description = "Persistencia no disponible.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PutMapping("/budget-periods/{periodId}/lines/{envelopeId}")
  public BudgetPeriodResponse upsertLine(
      @Parameter(
              description = "Identificador del hogar.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Quincena abierta del hogar.",
              required = true,
              example = "period-q1")
          @PathVariable
          String periodId,
      @Parameter(
              description = "Sobre activo del hogar.",
              required = true,
              example = "envelope-central")
          @PathVariable
          String envelopeId,
      @Valid @RequestBody UpsertPeriodBudgetLineRequest req) {
    return BudgetPeriodResponse.from(
        budget.upsertPeriodBudgetLine(householdId, periodId, req.toCommand(envelopeId)));
  }

  @Operation(
      operationId = "upsertTemplateBudgetLine",
      tags = "Budgets",
      summary = "Guardar presupuesto habitual de un sobre",
      description =
          "OWNER o EDITOR. Funciona sin quincena. Inserta o reemplaza solo la linea indicada,"
              + " conservando otras lineas e ingreso esperado. Crea la primera plantilla con"
              + " expectedIncome=0. Sobre y cuenta activos del hogar. Cero permitido. PUT"
              + " idempotente sin Idempotency-Key; no modifica saldos, movimientos ni snapshots."
              + " Comparte bloqueo del hogar con alta de sobres con plan, guardar plantilla y crear"
              + " periodos.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Plantilla actualizada.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BudgetTemplateResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Importe o referencia no disponibles.",
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
        description = "Sobre o cuenta no encontrados.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Conflicto concurrente; consultar y reintentar el PUT.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description = "Persistencia no disponible.",
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
  @PutMapping("/budget-template/lines/{envelopeId}")
  public BudgetTemplateResponse upsertTemplateLine(
      @Parameter(
              description = "Hogar con membresia activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId,
      @Parameter(
              description = "Sobre activo del hogar.",
              required = true,
              example = "envelope-central")
          @PathVariable
          String envelopeId,
      @Valid @RequestBody UpsertTemplateBudgetLineRequest req) {
    return BudgetTemplateResponse.from(
        budget.upsertTemplateBudgetLine(householdId, req.toCommand(envelopeId)));
  }
}

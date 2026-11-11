package dev.ioza.bank.infrastructure.adapters.in.controllers;

import dev.ioza.bank.application.ports.in.Dashboard;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.DashboardViewResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class DashboardController {
  private final Dashboard dashboard;

  public DashboardController(Dashboard dashboard) {
    this.dashboard = dashboard;
  }

  @Operation(
      operationId = "getDashboard",
      tags = "Dashboard",
      summary = "Consultar dinero total, reservado y libre",
      description =
          "Requiere membresía activa. total es dinero real; reserved es parte de total, nunca se"
              + " suman. free=total-reserved, también por cuenta.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Operación exitosa. Se conserva HTTP 200 en creaciones y replay.",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DashboardViewResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            "{\"total\":1080,\"reserved\":1080,\"free\":0,\"accounts\":[{\"id\":\"account-cash\",\"name\":\"Efectivo"
                                + " familiar\",\"type\":\"PHYSICAL\",\"total\":1080,\"reserved\":1080,\"free\":0}]}"))),
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
  @GetMapping("/dashboard")
  public DashboardViewResponse get(
      @Parameter(
              description = "Identificador del hogar; se verifica membresía activa.",
              required = true,
              example = "household-demo")
          @PathVariable
          String householdId) {
    return DashboardViewResponse.from(dashboard.get(householdId));
  }
}

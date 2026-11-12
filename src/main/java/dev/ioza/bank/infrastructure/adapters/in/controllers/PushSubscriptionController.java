
package dev.ioza.bank.infrastructure.adapters.in.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import dev.ioza.bank.application.ports.in.RegisterPushSubscriptionUseCase;
import dev.ioza.bank.application.ports.in.SendHouseholdNotificationUseCase;
import dev.ioza.bank.application.ports.out.RavenPushPort;
import dev.ioza.bank.application.services.AccessService;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.RegisterPushSubscriptionRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.request.SendHouseholdNotificationRequest;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.PushSubscriptionResponse;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.VapidPublicKeyResponse;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/households/{householdId}/push-subscriptions")
@Tag(name = "Push Subscriptions", description = "Administración de notificaciones Web Push de BANK")
public class PushSubscriptionController {

    private final RegisterPushSubscriptionUseCase registerUseCase;
    private final SendHouseholdNotificationUseCase sendUseCase;
    private final RavenPushPort ravenPushPort;
    private final AccessService accessService;

    public PushSubscriptionController(
            RegisterPushSubscriptionUseCase registerUseCase,
            SendHouseholdNotificationUseCase sendUseCase,
            RavenPushPort ravenPushPort,
            AccessService accessService) {

        this.registerUseCase = registerUseCase;
        this.sendUseCase = sendUseCase;
        this.ravenPushPort = ravenPushPort;
        this.accessService = accessService;
    }

    @PostMapping
    @Operation(operationId = "registerPushSubscription", summary = "Registrar dispositivo Web Push", description = """
            Registra una suscripción en Raven
            y la vincula al integrante autenticado
            dentro de su hogar.
            """)
    public Mono<ResponseEntity<PushSubscriptionResponse>> register(
            @PathVariable String householdId,
            @Valid @RequestBody RegisterPushSubscriptionRequest request) {

        accessService.requireMember(householdId);

        return registerUseCase
                .register(request.toCommand(householdId))
                .map(subscription -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(
                                PushSubscriptionResponse.from(
                                        subscription)));
    }

    @GetMapping("/vapid-public-key")
    @Operation(operationId = "getPushVapidPublicKey", summary = "Obtener clave pública VAPID", description = """
            Obtiene desde Raven la clave pública VAPID
            necesaria para generar suscripciones
            Web Push desde el navegador.
            """)
    public Mono<ResponseEntity<VapidPublicKeyResponse>> getVapidPublicKey(
            @PathVariable String householdId) {

        accessService.requireMember(householdId);

        return ravenPushPort.getVapidPublicKey()
                .map(publicKey -> ResponseEntity.ok(
                        new VapidPublicKeyResponse(publicKey)));
    }

    @PostMapping("/test-notification")
    @Operation(operationId = "sendTestPushNotification", summary = "Enviar notificación Web Push de prueba", description = """
            Envía una notificación de prueba
            a todos los dispositivos activos
            registrados en el hogar.

            Solo el propietario del hogar
            puede ejecutar esta operación.
            """)
    public Mono<ResponseEntity<Void>> sendTestNotification(
            @PathVariable String householdId,
            @Valid @RequestBody SendHouseholdNotificationRequest request) {

        accessService.requireOwner(householdId);

        return sendUseCase
                .send(request.toCommand(householdId))
                .thenReturn(
                        ResponseEntity
                                .accepted()
                                .<Void>build());
    }
}

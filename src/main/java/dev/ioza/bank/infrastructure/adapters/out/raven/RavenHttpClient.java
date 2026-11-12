
package dev.ioza.bank.infrastructure.adapters.out.raven;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dev.ioza.bank.infrastructure.adapters.out.raven.request.RavenSendNotificationRequest;
import dev.ioza.bank.infrastructure.adapters.out.raven.request.RegisterPushSubscriptionRequest;
import dev.ioza.bank.infrastructure.adapters.out.raven.response.RegisterPushSubscriptionResponse;
import dev.ioza.bank.infrastructure.config.RavenProperties;
import reactor.core.publisher.Mono;

@Component
public class RavenHttpClient {

    private static final String NOTIFICATION_KEY_HEADER = "X-Notification-Key";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final RavenProperties properties;

    public RavenHttpClient(
            WebClient.Builder builder,
            RavenProperties properties) {

        this.properties = properties;

        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public Mono<String> getVapidPublicKey() {

        return webClient.get()
                .uri("/api/push/vapid-public-key")
                .retrieve()
                .bodyToMono(VapidPublicKeyResponse.class)
                .map(VapidPublicKeyResponse::publicKey)
                .filter(key -> !key.isBlank())
                .switchIfEmpty(Mono.error(
                        new IllegalStateException(
                                "Raven no devolvió una clave VAPID válida")))
                .timeout(REQUEST_TIMEOUT);
    }

    public Mono<RegisterPushSubscriptionResponse> registerSubscription(
            RegisterPushSubscriptionRequest request) {

        return webClient.post()
                .uri("/api/push/subscriptions")
                .header(
                        NOTIFICATION_KEY_HEADER,
                        properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RegisterPushSubscriptionResponse.class)
                .filter(response -> response.subscriberKey() != null
                        && !response.subscriberKey().isBlank())
                .switchIfEmpty(Mono.error(
                        new IllegalStateException(
                                "Raven no devolvió un subscriberKey válido")))
                .timeout(REQUEST_TIMEOUT);
    }

    private record VapidPublicKeyResponse(
            String publicKey) {
    }

    public Mono<Void> sendNotification(
            RavenSendNotificationRequest request) {

        return Mono.defer(() -> {

            if (properties.apiKey() == null
                    || properties.apiKey().isBlank()) {
                return Mono.error(new IllegalStateException(
                        "La API Key de Raven no está configurada"));
            }

            if (request == null
                    || request.targets() == null
                    || request.targets().isEmpty()) {
                return Mono.error(new IllegalArgumentException(
                        "La notificación debe tener destinatarios"));
            }

            return webClient.post()
                    .uri("/api/notifications/send")
                    .header(
                            NOTIFICATION_KEY_HEADER,
                            properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.value() != 202,
                            response -> response.createException())
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .then();
        });
    }

}


package dev.ioza.bank.infrastructure.adapters.out.raven;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import dev.ioza.bank.application.ports.out.RavenPushPort;
import dev.ioza.bank.infrastructure.adapters.out.raven.request.PushSubscriptionKeysRequest;
import dev.ioza.bank.infrastructure.adapters.out.raven.request.RavenNotificationTargetRequest;
import dev.ioza.bank.infrastructure.adapters.out.raven.request.RavenSendNotificationRequest;
import dev.ioza.bank.infrastructure.adapters.out.raven.request.RegisterPushSubscriptionRequest;
import dev.ioza.bank.infrastructure.adapters.out.raven.response.RegisterPushSubscriptionResponse;
import dev.ioza.bank.infrastructure.config.RavenProperties;
import reactor.core.publisher.Mono;

@Component
public class RavenPushAdapter implements RavenPushPort {

    private final RavenHttpClient ravenHttpClient;
    private final RavenProperties properties;

    public RavenPushAdapter(
            RavenHttpClient ravenHttpClient,
            RavenProperties properties) {

        this.ravenHttpClient = ravenHttpClient;
        this.properties = properties;
    }

    @Override
    public Mono<String> getVapidPublicKey() {
        return ravenHttpClient.getVapidPublicKey();
    }

    @Override
    public Mono<String> registerSubscription(
            String endpoint,
            String p256dh,
            String auth,
            String externalUserId) {

        return Mono.defer(() -> {

            RegisterPushSubscriptionRequest request = new RegisterPushSubscriptionRequest(
                    properties.appId(),
                    endpoint,
                    new PushSubscriptionKeysRequest(
                            p256dh,
                            auth),
                    externalUserId);

            return ravenHttpClient.registerSubscription(request)
                    .map(RegisterPushSubscriptionResponse::subscriberKey);
        });
    }

    @Override
    public Mono<Void> sendNotification(
            String notificationId,
            String title,
            String message,
            Map<String, Object> metadata,
            List<String> subscriberKeys) {

        return Mono.defer(() -> {

            if (notificationId == null || notificationId.isBlank()) {
                return Mono.error(new IllegalArgumentException(
                        "notificationId es obligatorio"));
            }

            if (message == null || message.isBlank()) {
                return Mono.error(new IllegalArgumentException(
                        "El mensaje es obligatorio"));
            }

            if (subscriberKeys == null || subscriberKeys.isEmpty()) {
                return Mono.empty();
            }

            List<String> destinations = subscriberKeys.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(key -> !key.isBlank())
                    .distinct()
                    .toList();

            if (destinations.isEmpty()) {
                return Mono.empty();
            }

            List<RavenNotificationTargetRequest> targets = destinations.stream()
                    .map(subscriberKey -> new RavenNotificationTargetRequest(
                            properties.integrationId(),
                            subscriberKey,
                            Map.of(
                                    "url", "/dashboard",
                                    "ttl", 300)))
                    .toList();

            RavenSendNotificationRequest request = new RavenSendNotificationRequest(
                    notificationId,
                    "bank",
                    null,
                    title,
                    message,
                    metadata == null ? Map.of() : metadata,
                    targets);

            return ravenHttpClient.sendNotification(request);
        });
    }
}

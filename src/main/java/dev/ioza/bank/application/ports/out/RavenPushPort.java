package dev.ioza.bank.application.ports.out;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

public interface RavenPushPort {

    Mono<String> getVapidPublicKey();

    Mono<String> registerSubscription(
            String endpoint,
            String p256dh,
            String auth,
            String externalUserId);

    Mono<Void> sendNotification(
            String notificationId,
            String title,
            String message,
            Map<String, Object> metadata,
            List<String> subscriberKeys);

}

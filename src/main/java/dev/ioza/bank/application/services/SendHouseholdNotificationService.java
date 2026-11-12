
package dev.ioza.bank.application.services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import dev.ioza.bank.application.commands.SendHouseholdNotificationCommand;
import dev.ioza.bank.application.ports.in.SendHouseholdNotificationUseCase;
import dev.ioza.bank.application.ports.out.PushSubscriptions;
import dev.ioza.bank.application.ports.out.RavenPushPort;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.PushSubscription;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class SendHouseholdNotificationService
        implements SendHouseholdNotificationUseCase {

    private final PushSubscriptions pushSubscriptions;
    private final RavenPushPort ravenPushPort;

    public SendHouseholdNotificationService(
            PushSubscriptions pushSubscriptions,
            RavenPushPort ravenPushPort) {

        this.pushSubscriptions = pushSubscriptions;
        this.ravenPushPort = ravenPushPort;
    }

    @Override
    public Mono<Void> send(
            SendHouseholdNotificationCommand command) {

        return Mono.defer(() -> {

            validate(command);

            return Mono.fromCallable(() -> findActiveSubscriberKeys(
                    command.householdId()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(subscriberKeys -> {

                        if (subscriberKeys.isEmpty()) {
                            return Mono.<Void>empty();
                        }

                        return ravenPushPort.sendNotification(
                                command.notificationId(),
                                command.title(),
                                command.message(),
                                command.metadata(),
                                subscriberKeys);
                    });
        });
    }

    private List<String> findActiveSubscriberKeys(
            String householdId) {

        return pushSubscriptions
                .findByHouseholdIdAndActiveTrue(householdId)
                .stream()
                .map(PushSubscription::subscriberKey)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isBlank())
                .distinct()
                .toList();
    }

    private void validate(
            SendHouseholdNotificationCommand command) {

        if (command == null) {
            throw new ValidationException(
                    "El comando de notificación es obligatorio");
        }

        required(command.householdId(), "householdId");
        required(command.notificationId(), "notificationId");
        required(command.title(), "title");
        required(command.message(), "message");
    }

    private void required(
            String value,
            String field) {

        if (value == null || value.isBlank()) {
            throw new ValidationException(
                    field + " es obligatorio");
        }
    }
}

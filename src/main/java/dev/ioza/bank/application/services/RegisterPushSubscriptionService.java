
package dev.ioza.bank.application.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.ioza.bank.application.commands.RegisterPushSubscriptionCommand;
import dev.ioza.bank.application.ports.in.RegisterPushSubscriptionUseCase;
import dev.ioza.bank.application.ports.out.CurrentUser;
import dev.ioza.bank.application.ports.out.Members;
import dev.ioza.bank.application.ports.out.PushSubscriptions;
import dev.ioza.bank.application.ports.out.RavenPushPort;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.domain.model.HouseholdMember;
import dev.ioza.bank.domain.model.PushSubscription;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class RegisterPushSubscriptionService
        implements RegisterPushSubscriptionUseCase {

    private final CurrentUser currentUser;
    private final Members members;
    private final RavenPushPort ravenPushPort;
    private final PushSubscriptions pushSubscriptions;

    public RegisterPushSubscriptionService(
            CurrentUser currentUser,
            Members members,
            RavenPushPort ravenPushPort,
            PushSubscriptions pushSubscriptions) {

        this.currentUser = currentUser;
        this.members = members;
        this.ravenPushPort = ravenPushPort;
        this.pushSubscriptions = pushSubscriptions;
    }

    @Override
    public Mono<PushSubscription> register(
            RegisterPushSubscriptionCommand command) {

        // Capturar identidad en el hilo autenticado de Spring MVC.
        String username = currentUser.username();

        return Mono.fromCallable(() -> validateAndPrepare(command, username))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(context -> ravenPushPort.registerSubscription(
                        command.endpoint(),
                        command.p256dh(),
                        command.auth(),
                        context.memberId()).flatMap(
                                subscriberKey -> Mono.fromCallable(() -> persist(
                                        command,
                                        context,
                                        subscriberKey)).subscribeOn(
                                                Schedulers.boundedElastic())));
    }

    private RegistrationContext validateAndPrepare(
            RegisterPushSubscriptionCommand command,
            String username) {

        required(command.householdId(), "householdId");
        required(command.installationId(), "installationId");
        required(command.deviceName(), "deviceName");
        required(command.endpoint(), "endpoint");
        required(command.p256dh(), "p256dh");
        required(command.auth(), "auth");

        HouseholdMember member = members.find(
                command.householdId(),
                username)
                .filter(HouseholdMember::active)
                .orElseThrow(() -> new ValidationException(
                        "No tienes una membresía activa en este hogar"));

        PushSubscription existing = pushSubscriptions
                .findByHouseholdIdAndInstallationId(
                        command.householdId(),
                        command.installationId())
                .orElse(null);

        if (existing != null
                && !existing.memberId().equals(member.id())) {

            throw new ConflictException(
                    "Esta instalación pertenece a otro integrante");
        }

        return new RegistrationContext(
                member.id(),
                existing);
    }

    private PushSubscription persist(
            RegisterPushSubscriptionCommand command,
            RegistrationContext context,
            String subscriberKey) {

        if (subscriberKey == null || subscriberKey.isBlank()) {
            throw new IllegalStateException(
                    "Raven no devolvió un subscriberKey válido");
        }

        PushSubscription existingByKey = pushSubscriptions
                .findBySubscriberKey(subscriberKey)
                .orElse(null);

        if (existingByKey != null
                && !existingByKey.memberId().equals(context.memberId())) {

            throw new ConflictException(
                    "La suscripción pertenece a otro integrante");
        }

        if (existingByKey != null
                && !existingByKey.householdId()
                        .equals(command.householdId())) {

            throw new ConflictException(
                    "La suscripción pertenece a otro hogar");
        }

        if (existingByKey != null
                && !existingByKey.installationId()
                        .equals(command.installationId())) {

            throw new ConflictException(
                    "La suscripción pertenece a otra instalación");
        }

        PushSubscription previous = context.existing();

        if (previous != null
                && existingByKey != null
                && !previous.id().equals(existingByKey.id())) {

            throw new ConflictException(
                    "La suscripción ya está registrada");
        }

        Instant now = Instant.now();

        PushSubscription subscription = new PushSubscription(
                previous != null
                        ? previous.id()
                        : UUID.randomUUID().toString(),
                command.householdId(),
                context.memberId(),
                subscriberKey,
                command.deviceName().trim(),
                command.installationId().trim(),
                true,
                previous != null
                        ? previous.createdAt()
                        : now,
                now);

        return pushSubscriptions.save(subscription);
    }

    private static void required(
            String value,
            String field) {

        if (value == null || value.isBlank()) {
            throw new ValidationException(
                    field + " es obligatorio");
        }
    }

    private record RegistrationContext(
            String memberId,
            PushSubscription existing) {
    }
}

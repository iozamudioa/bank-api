
package dev.ioza.bank.infrastructure.adapters.out.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import dev.ioza.bank.application.ports.out.PushSubscriptions;
import dev.ioza.bank.domain.model.PushSubscription;
import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.PushSubscriptionDoc;
import dev.ioza.bank.infrastructure.adapters.out.mongo.repositories.PushSubscriptionMongoRepository;

@Repository
public class PushSubscriptionMongoAdapter implements PushSubscriptions {

    private final PushSubscriptionMongoRepository repository;

    public PushSubscriptionMongoAdapter(
            PushSubscriptionMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public PushSubscription save(PushSubscription subscription) {
        PushSubscriptionDoc document = toDocument(subscription);

        PushSubscriptionDoc saved = repository.save(document);

        return toDomain(saved);
    }

    @Override
    public Optional<PushSubscription> findById(String id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<PushSubscription> findBySubscriberKey(
            String subscriberKey) {
        return repository.findBySubscriberKey(subscriberKey)
                .map(this::toDomain);
    }

    @Override
    public Optional<PushSubscription> findByHouseholdIdAndInstallationId(
            String householdId,
            String installationId) {

        return repository.findByHouseholdIdAndInstallationId(
                householdId,
                installationId)
                .map(this::toDomain);
    }

    @Override
    public List<PushSubscription> findByHouseholdId(
            String householdId) {

        return repository.findByHouseholdId(householdId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PushSubscription> findByHouseholdIdAndActiveTrue(
            String householdId) {

        return repository.findByHouseholdIdAndActiveTrue(householdId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private PushSubscriptionDoc toDocument(
            PushSubscription subscription) {

        return new PushSubscriptionDoc(
                subscription.id(),
                subscription.householdId(),
                subscription.memberId(),
                subscription.subscriberKey(),
                subscription.deviceName(),
                subscription.installationId(),
                subscription.active(),
                subscription.createdAt(),
                subscription.updatedAt());
    }

    private PushSubscription toDomain(
            PushSubscriptionDoc document) {

        return new PushSubscription(
                document.id(),
                document.householdId(),
                document.memberId(),
                document.subscriberKey(),
                document.deviceName(),
                document.installationId(),
                document.active(),
                document.createdAt(),
                document.updatedAt());
    }
}

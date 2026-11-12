
package dev.ioza.bank.infrastructure.adapters.out.mongo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import dev.ioza.bank.infrastructure.adapters.out.mongo.documents.PushSubscriptionDoc;

public interface PushSubscriptionMongoRepository
        extends MongoRepository<PushSubscriptionDoc, String> {

    Optional<PushSubscriptionDoc> findBySubscriberKey(
            String subscriberKey);

    Optional<PushSubscriptionDoc> findByHouseholdIdAndInstallationId(
            String householdId,
            String installationId);

    List<PushSubscriptionDoc> findByHouseholdId(
            String householdId);

    List<PushSubscriptionDoc> findByHouseholdIdAndActiveTrue(
            String householdId);
}

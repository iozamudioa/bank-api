package dev.ioza.bank.application.ports.out;

import java.util.List;
import java.util.Optional;

import dev.ioza.bank.domain.model.PushSubscription;

public interface PushSubscriptions {

    PushSubscription save(PushSubscription subscription);

    Optional<PushSubscription> findById(String id);

    Optional<PushSubscription> findBySubscriberKey(String subscriberKey);

    Optional<PushSubscription> findByHouseholdIdAndInstallationId(
            String householdId,
            String installationId);

    List<PushSubscription> findByHouseholdId(String householdId);

    List<PushSubscription> findByHouseholdIdAndActiveTrue(
            String householdId);

}

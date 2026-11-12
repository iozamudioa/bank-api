
package dev.ioza.bank.infrastructure.adapters.in.controllers.response;

import java.time.Instant;

import dev.ioza.bank.domain.model.PushSubscription;

public record PushSubscriptionResponse(
        String id,
        String householdId,
        String memberId,
        String subscriberKey,
        String deviceName,
        String installationId,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static PushSubscriptionResponse from(
            PushSubscription subscription) {

        return new PushSubscriptionResponse(
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
}

package dev.ioza.bank.domain.model;

import java.time.Instant;

public record PushSubscription(String id,
        String householdId,
        String memberId,
        String subscriberKey,
        String deviceName,
        String installationId,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

}


package dev.ioza.bank.infrastructure.adapters.out.mongo.documents;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "push_subscriptions")
@CompoundIndexes({
        @CompoundIndex(name = "uk_push_household_installation", def = "{'householdId': 1, 'installationId': 1}", unique = true),
        @CompoundIndex(name = "idx_push_household_active", def = "{'householdId': 1, 'active': 1}")
})
public record PushSubscriptionDoc(

        @Id String id,

        String householdId,

        String memberId,

        @Indexed(name = "uk_push_subscriber_key", unique = true) String subscriberKey,

        String deviceName,

        String installationId,

        boolean active,

        Instant createdAt,

        Instant updatedAt

) {
}

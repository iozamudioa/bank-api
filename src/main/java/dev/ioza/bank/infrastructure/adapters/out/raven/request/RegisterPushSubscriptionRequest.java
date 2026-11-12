package dev.ioza.bank.infrastructure.adapters.out.raven.request;

public record RegisterPushSubscriptionRequest(
        String appId,
        String endpoint,
        PushSubscriptionKeysRequest keys,
        String externalUserId) {
}
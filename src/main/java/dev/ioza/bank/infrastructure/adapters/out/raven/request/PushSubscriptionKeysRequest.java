package dev.ioza.bank.infrastructure.adapters.out.raven.request;

public record PushSubscriptionKeysRequest(
        String p256dh,
        String auth) {
}
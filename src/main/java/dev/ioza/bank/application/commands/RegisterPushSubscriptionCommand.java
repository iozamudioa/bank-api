
package dev.ioza.bank.application.commands;

public record RegisterPushSubscriptionCommand(
        String householdId,
        String installationId,
        String deviceName,
        String endpoint,
        String p256dh,
        String auth) {
}
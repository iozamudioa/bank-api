
package dev.ioza.bank.application.commands;

import java.util.Map;

public record SendHouseholdNotificationCommand(
        String householdId,
        String notificationId,
        String title,
        String message,
        Map<String, Object> metadata) {

    public SendHouseholdNotificationCommand {
        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}

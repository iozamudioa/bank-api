
package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import java.util.Map;
import java.util.UUID;

import dev.ioza.bank.application.commands.SendHouseholdNotificationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendHouseholdNotificationRequest(

        @NotBlank(message = "title es obligatorio") @Size(max = 150) String title,

        @NotBlank(message = "message es obligatorio") @Size(max = 1000) String message,

        Map<String, Object> metadata

) {

    public SendHouseholdNotificationCommand toCommand(
            String householdId) {

        return new SendHouseholdNotificationCommand(
                householdId,
                UUID.randomUUID().toString(),
                title,
                message,
                metadata == null ? Map.of() : metadata);
    }
}

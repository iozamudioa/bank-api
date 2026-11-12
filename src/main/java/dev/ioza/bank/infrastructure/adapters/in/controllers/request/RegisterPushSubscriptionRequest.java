
package dev.ioza.bank.infrastructure.adapters.in.controllers.request;

import dev.ioza.bank.application.commands.RegisterPushSubscriptionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPushSubscriptionRequest(

        @NotBlank(message = "installationId es obligatorio") @Size(max = 128) String installationId,
        @NotBlank(message = "deviceName es obligatorio") @Size(max = 100) String deviceName,
        @NotBlank(message = "endpoint es obligatorio") String endpoint,
        @NotBlank(message = "p256dh es obligatorio") String p256dh,
        @NotBlank(message = "auth es obligatorio") String auth

) {

    public RegisterPushSubscriptionCommand toCommand(
            String householdId) {

        return new RegisterPushSubscriptionCommand(
                householdId,
                installationId,
                deviceName,
                endpoint,
                p256dh,
                auth);
    }
}

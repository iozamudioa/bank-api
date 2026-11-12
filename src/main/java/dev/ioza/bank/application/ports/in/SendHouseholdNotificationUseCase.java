
package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.application.commands.SendHouseholdNotificationCommand;
import reactor.core.publisher.Mono;

public interface SendHouseholdNotificationUseCase {

    Mono<Void> send(SendHouseholdNotificationCommand command);

}


package dev.ioza.bank.application.ports.in;

import dev.ioza.bank.application.commands.RegisterPushSubscriptionCommand;
import dev.ioza.bank.domain.model.PushSubscription;
import reactor.core.publisher.Mono;

public interface RegisterPushSubscriptionUseCase {

    Mono<PushSubscription> register(
            RegisterPushSubscriptionCommand command);
}

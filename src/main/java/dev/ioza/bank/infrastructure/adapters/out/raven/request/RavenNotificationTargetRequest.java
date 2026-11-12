
package dev.ioza.bank.infrastructure.adapters.out.raven.request;

import java.util.Map;

public record RavenNotificationTargetRequest(
        String integrationId,
        String destination,
        Map<String, Object> options) {
}

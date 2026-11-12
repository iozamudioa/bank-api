
package dev.ioza.bank.infrastructure.adapters.out.raven.request;

import java.util.List;
import java.util.Map;

public record RavenSendNotificationRequest(
        String id,
        String source,
        String templateId,
        String title,
        String message,
        Map<String, Object> metadata,
        List<RavenNotificationTargetRequest> targets) {
}

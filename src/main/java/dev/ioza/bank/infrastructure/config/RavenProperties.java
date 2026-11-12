package dev.ioza.bank.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank.raven")
public record RavenProperties(String baseUrl,
        String apiKey,
        String appId,
        String integrationId) {

}

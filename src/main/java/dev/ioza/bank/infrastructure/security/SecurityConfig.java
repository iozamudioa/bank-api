package dev.ioza.bank.infrastructure.security;

import dev.ioza.bank.application.ports.out.CurrentUser;
import dev.ioza.bank.domain.exceptions.ForbiddenException;
import dev.ioza.kingdom.contracts.identity.KingdomIdentityProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {
  @Bean
  CurrentUser currentUser(KingdomIdentityProvider provider) {
    return () -> provider.current()
        .filter(identity -> identity.isKnight())
        .orElseThrow(() -> new ForbiddenException("Se requiere una identidad Knight"))
        .subject();
  }
}

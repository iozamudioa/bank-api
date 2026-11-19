package dev.ioza.bank.infrastructure.security;

import dev.ioza.bank.application.ports.out.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Business identity is supplied by Kingdom Tavern Lock's validated Parchment. */
@Configuration
public class SecurityConfig {
  @Bean
  CurrentUser currentUser() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()
          || "anonymousUser".equals(authentication.getPrincipal())) {
        throw new IllegalStateException("No authenticated Kingdom Knight");
      }
      return authentication.getName();
    };
  }
}

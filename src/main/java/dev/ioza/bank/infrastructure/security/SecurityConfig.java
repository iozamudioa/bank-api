package dev.ioza.bank.infrastructure.security;

import dev.ioza.bank.application.ports.out.CurrentUser;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Temporary local/WireGuard authentication. Replace with OIDC / proper multi-user auth before
 * public access.
 */
@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper mapper) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(
            b ->
                b.authenticationEntryPoint(
                    (request, response, error) -> {
                      response.setStatus(401);
                      response.setHeader("WWW-Authenticate", "Basic realm=\"BANK\"");
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      response
                          .getOutputStream()
                          .write(
                              mapper.writeValueAsBytes(
                                  new ErrorResponse(
                                      "UNAUTHORIZED",
                                      "Autenticación HTTP Basic requerida o credenciales"
                                          + " inválidas")));
                    }))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (request, response, error) -> {
                      response.setStatus(403);
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      response
                          .getOutputStream()
                          .write(
                              mapper.writeValueAsBytes(
                                  new ErrorResponse(
                                      "FORBIDDEN", "Sin permisos para esta operación")));
                    }));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(
      PasswordEncoder encoder,
      @Value("${bank.auth.owner-user:olda}") String ownerUsername,
      @Value("${bank.auth.owner-password:}") String ownerPassword,
      @Value("${bank.auth.cecy-user:cecy}") String cecyUsername,
      @Value("${bank.auth.cecy-password:}") String cecyPassword) {
    if (ownerPassword.isBlank() || cecyPassword.isBlank() || ownerUsername.equals(cecyUsername))
      throw new IllegalStateException(
          "Configura BANK_OWNER_PASSWORD y BANK_CECY_PASSWORD distintos antes de arrancar");
    return new InMemoryUserDetailsManager(
        User.withUsername(ownerUsername)
            .password(encoder.encode(ownerPassword))
            .roles("USER")
            .build(),
        User.withUsername(cecyUsername)
            .password(encoder.encode(cecyPassword))
            .roles("USER")
            .build());
  }

  @Bean
  CurrentUser currentUser() {
    return () -> SecurityContextHolder.getContext().getAuthentication().getName();
  }
}

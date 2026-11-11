package dev.ioza.bank.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

class ArchitectureTest {
  final Path root = Path.of("src/main/java/dev/ioza/bank");

  @Test
  void onePublicTypePerFileAndExplicitImports() throws Exception {
    Pattern declaration =
        Pattern.compile("(?m)^public (?:final )?(?:class|record|interface|enum) (\\w+)");
    try (var paths = Files.walk(root)) {
      for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(path);
        var matches = declaration.matcher(source).results().toList();
        assertEquals(1, matches.size(), path.toString());
        assertEquals(
            path.getFileName().toString().replace(".java", ""),
            matches.get(0).group(1),
            path.toString());
        assertFalse(Pattern.compile("(?m)^import .*\\*;").matcher(source).find(), path.toString());
        String body = source.replaceAll("(?m)^import [^\\n]+\\n", "");
        assertFalse(
            Pattern.compile("\\bjava\\.(util|math|time)\\.[A-Z]").matcher(body).find(),
            path.toString());
      }
    }
  }

  @Test
  void dependenciesPointTowardDomainAndInputPorts() throws Exception {
    try (var paths = Files.walk(root)) {
      for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(path),
            relative = root.relativize(path).toString().replace('\\', '/');
        if (relative.contains("/")) {
          assertTrue(
              Set.of("domain", "application", "infrastructure").contains(relative.split("/")[0]),
              relative);
        }
        if (relative.startsWith("domain/")) {
          assertFalse(
              Pattern.compile(
                      "import"
                          + " (org\\.springframework|com\\.mongodb|org\\.bson|jakarta\\.|dev\\.ioza\\.bank\\.(infrastructure|adapters|application|config|exceptions))")
                  .matcher(source)
                  .find(),
              relative);
        }
        if (relative.startsWith("application/")) {
          assertFalse(
              Pattern.compile("dev\\.ioza\\.bank\\.(infrastructure|adapters|config|exceptions)\\.")
                  .matcher(source)
                  .find(),
              relative);
        }
        if (relative.startsWith("application/commands/")) {
          assertFalse(source.contains("jakarta.validation"), relative);
        }
        if (relative.startsWith("infrastructure/adapters/in/controllers/")) {
          assertFalse(source.contains("dev.ioza.bank.application.services"), relative);
          assertFalse(source.contains("dev.ioza.bank.infrastructure.adapters.out"), relative);
        }
      }
    }
  }

  @Test
  void relocatedComponentsRemainDiscoverableForDependencyInjection() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(true);
    var components = scanner.findCandidateComponents("dev.ioza.bank");
    Set<String> names =
        components.stream().map(b -> b.getBeanClassName()).collect(Collectors.toSet());
    for (String name :
        List.of(
            "infrastructure.config.InfrastructureConfig",
            "infrastructure.security.SecurityConfig",
            "infrastructure.adapters.in.controllers.MovementController",
            "infrastructure.adapters.in.controllers.advice.ApiExceptionHandler")) {
      assertTrue(names.contains("dev.ioza.bank." + name), name);
    }
    for (String port :
        List.of(
            "in.Setup",
            "in.Finance",
            "in.Budget",
            "in.Dashboard",
            "in.IdempotentMovements",
            "out.Accounts",
            "out.Envelopes",
            "out.Households",
            "out.Members",
            "out.Incomes",
            "out.Budgets",
            "out.Movements",
            "out.IdempotencyStore",
            "out.CommandFingerprint",
            "out.EnvelopeCreations")) {
      Class<?> contract = Class.forName("dev.ioza.bank.application.ports." + port);
      int implementations = 0;
      for (String name : names) {
        if (contract.isAssignableFrom(Class.forName(name, false, getClass().getClassLoader()))) {
          implementations++;
        }
      }
      assertEquals(1, implementations, port);
    }
  }
}

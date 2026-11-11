package dev.ioza.bank.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
  @Bean
  OpenAPI bankOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("BANK API")
                .version("1.0.0")
                .description(
                    "API para administración financiera familiar mediante cuentas, sobres,"
                        + " presupuestos y movimientos."))
        .addServersItem(new Server().url("/").description("Mismo origen; recursos bajo /api/v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "basicAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
  }

  /** Flatten generated interface inheritance so oneOf never recursively refers back to itself. */
  @Bean
  OpenApiCustomizer correctionReplacementSchemas() {
    return api -> {
      var schemas = api.getComponents().getSchemas();
      var union = schemas.get("MovementReplacementRequest");
      if (union == null) return;
      Map<String, String> variants =
          Map.of(
              "INCOME",
              "IncomeReplacementRequest",
              "ALLOCATE",
              "AllocationReplacementRequest",
              "RELEASE",
              "ReleaseReplacementRequest",
              "REALLOCATE",
              "ReallocationReplacementRequest",
              "TRANSFER",
              "TransferReplacementRequest",
              "ENVELOPE_TRANSFER",
              "EnvelopeTransferReplacementRequest",
              "EXPENSE",
              "ExpenseReplacementRequest",
              "ACCOUNT_EXPENSE",
              "AccountExpenseReplacementRequest");
      variants.forEach(
          (type, name) -> {
            Schema<?> generated = schemas.get(name);
            var flat = new ObjectSchema();
            flat.setDescription(generated.getDescription());
            flat.setExample(generated.getExample());
            if (generated.getProperties() != null)
              generated.getProperties().forEach(flat::addProperty);
            if (generated.getAllOf() != null)
              generated
                  .getAllOf()
                  .forEach(
                      rawPart -> {
                        Schema<?> part = rawPart;
                        if (part.getProperties() != null)
                          part.getProperties().forEach(flat::addProperty);
                      });
            flat.addProperty("type", new StringSchema()._enum(List.of(type)));
            if (generated.getRequired() != null)
              generated.getRequired().forEach(flat::addRequiredItem);
            flat.addRequiredItem("type");
            schemas.put(name, flat);
            union.getDiscriminator().mapping(type, "#/components/schemas/" + name);
          });
    };
  }
}

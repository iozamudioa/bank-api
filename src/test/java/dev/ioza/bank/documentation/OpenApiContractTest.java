package dev.ioza.bank.documentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ioza.bank.application.ports.in.Budget;
import dev.ioza.bank.application.ports.in.Dashboard;
import dev.ioza.bank.application.ports.in.Finance;
import dev.ioza.bank.application.ports.in.IdempotentMovements;
import dev.ioza.bank.application.ports.in.Setup;
import dev.ioza.bank.infrastructure.adapters.in.controllers.AccountController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.BudgetController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.DashboardController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.EnvelopeController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.HouseholdController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.MovementController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.advice.ApiExceptionHandler;
import dev.ioza.bank.infrastructure.config.OpenApiConfig;
import dev.ioza.bank.infrastructure.security.SecurityConfig;
import jakarta.validation.Validation;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real HTTP, MVC, security and Springdoc; business ports cannot perform financial operations.
 * MongoDB is deliberately excluded only in this documentation test, never in the production app.
 */
@SpringBootTest(
    classes = OpenApiContractTest.DocumentationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "bank.auth.owner-password=documentation-owner",
      "bank.auth.cecy-password=documentation-editor",
      "logging.level.root=WARN",
      "debug=false"
    })
class OpenApiContractTest {
  @Autowired Environment environment;
  @Autowired JsonMapper mapper;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

  HttpResponse<String> get(String path, boolean auth) throws Exception {
    var request =
        HttpRequest.newBuilder(
            URI.create("http://localhost:" + environment.getProperty("local.server.port") + path));
    if (auth)
      request.header(
          "Authorization",
          "Basic "
              + Base64.getEncoder()
                  .encodeToString("olda:documentation-owner".getBytes(StandardCharsets.UTF_8)));
    return client.send(
        request.GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  @Test
  void exportedYamlMatchesLiveJsonAndEveryReferenceResolves() throws Exception {
    var json = get("/v3/api-docs", false);
    var yaml = get("/v3/api-docs.yaml", false);
    assertEquals(200, json.statusCode(), json.body());
    assertEquals(200, yaml.statusCode(), yaml.body());
    var tree = mapper.readTree(json.body());
    assertDocumentsEqual(tree, mapper.valueToTree(yaml().load(yaml.body())), "live YAML");
    assertEquals("BANK API", tree.at("/info/title").asString());
    assertEquals("1.0.0", tree.at("/info/version").asString());
    assertEquals("basic", tree.at("/components/securitySchemes/basicAuth/scheme").asString());
    var ids = new HashSet<String>();
    var documented = new HashSet<String>();
    int operations = 0;
    for (var path : tree.get("paths").properties()) {
      assertTrue(path.getKey().startsWith("/api/v1/"));
      for (var operation : path.getValue().properties()) {
        if (!List.of("get", "post", "put").contains(operation.getKey())) continue;
        var value = operation.getValue();
        documented.add(operation.getKey().toUpperCase() + " " + path.getKey());
        assertTrue(ids.add(value.get("operationId").asString()), value.toString());
        assertFalse(value.get("summary").asString().isBlank());
        assertNotNull(value.get("responses").get("200"));
        assertNotNull(value.get("responses").get("401"));
        assertNotNull(value.at("/responses/200/content/application~1json/schema"));
        assertFalse(value.at("/responses/200/content/application~1json/schema").isMissingNode());
        operations++;
      }
    }
    assertEquals(37, operations);
    var actual = new HashSet<String>();
    mappings
        .getHandlerMethods()
        .forEach(
            (mapping, method) -> {
              for (String path : mapping.getPatternValues()) {
                if (path.startsWith("/api/v1/")) {
                  mapping
                      .getMethodsCondition()
                      .getMethods()
                      .forEach(verb -> actual.add(verb.name() + " " + path));
                }
              }
            });
    assertEquals(actual, documented);
    validateReferences(tree, tree);
    Path exported = Path.of("docs/openapi/bank-api-v1.yaml");
    if (Boolean.getBoolean("bank.openapi.export")) {
      Files.createDirectories(exported.getParent());
      Files.writeString(exported, yaml.body(), StandardCharsets.UTF_8);
    }
    assertTrue(
        Files.exists(exported),
        "Generar: mvn -Dtest=OpenApiContractTest -Dbank.openapi.export=true test");
    assertDocumentsEqual(
        tree, mapper.valueToTree(yaml().load(Files.readString(exported))), "export");
  }

  @Test
  void correctionUnionHasExplicitDiscriminatorAndNonRecursiveTypedVariants() throws Exception {
    var spec = mapper.readTree(get("/v3/api-docs", false).body());
    var union = spec.at("/components/schemas/MovementReplacementRequest");
    assertEquals("type", union.at("/discriminator/propertyName").asString());
    assertEquals(8, union.get("oneOf").size());
    for (var variant : union.at("/discriminator/mapping").properties()) {
      var schema = spec.at(variant.getValue().asString().substring(1));
      assertFalse(schema.has("allOf"));
      assertEquals(variant.getKey(), schema.at("/properties/type/enum/0").asString());
      assertTrue(schema.get("required").toString().contains("type"));
      assertEquals("number", schema.at("/properties/amount/type").asString());
    }
  }

  @Test
  void habitualPlanContractIsOptionalOnCreateAndDerivedInEnvelopeResponses() throws Exception {
    var spec = mapper.readTree(get("/v3/api-docs", false).body());
    var request = spec.at("/components/schemas/CreateEnvelopeRequest");
    assertFalse(request.get("required").toString().contains("habitualAmount"));
    assertEquals("number", request.at("/properties/habitualAmount/type").asString());
    assertEquals("decimal", request.at("/properties/habitualAmount/format").asString());
    assertTrue(request.at("/properties/habitualAmount/nullable").asBoolean());
    assertEquals(
        "number",
        spec.at("/components/schemas/EnvelopeResponse/properties/habitualAmount/type").asString());
    assertTrue(
        spec.at("/components/schemas/EnvelopeResponse/properties/habitualAmount/nullable")
            .asBoolean());
    assertTrue(
        spec.at("/components/schemas/EnvelopeResponse/properties/habitualAmount/readOnly")
            .asBoolean());
    boolean found = false;
    for (var parameter :
        spec.at("/paths/~1api~1v1~1households~1{householdId}~1envelopes/post/parameters")) {
      if (parameter.get("name").asString().equals("Idempotency-Key")) {
        found = true;
        assertFalse(parameter.path("required").asBoolean());
      }
    }
    assertTrue(found);
  }

  @Test
  void onlyReplacementAmountsPermitZeroAndExplicitResourcesAreDocumented() throws Exception {
    var spec = mapper.readTree(get("/v3/api-docs", false).body());
    for (var kind :
        List.of(
            "Income",
            "Allocation",
            "Release",
            "Reallocation",
            "Transfer",
            "EnvelopeTransfer",
            "Expense",
            "AccountExpense")) {
      var schema = spec.at("/components/schemas/" + kind + "ReplacementRequest/properties/amount");
      assertEquals(0, schema.get("minimum").decimalValue().signum(), kind);
    }
    for (var kind :
        List.of(
            "Income",
            "Allocation",
            "Reallocation",
            "Transfer",
            "EnvelopeTransfer",
            "Expense",
            "AccountExpense",
            "Funding")) {
      var schema = spec.at("/components/schemas/" + kind + "Request/properties/amount");
      assertTrue(schema.get("minimum").decimalValue().signum() > 0, kind);
    }
    assertFalse(
        spec.at("/components/schemas/MovementResponse/properties/replacement").isMissingNode());
    var resources = spec.at("/components/schemas/MovementReplacementResponse/properties");
    for (var field :
        List.of(
            "accountId",
            "envelopeId",
            "fromAccountId",
            "toAccountId",
            "fromEnvelopeId",
            "toEnvelopeId",
            "targetEnvelopeId",
            "incomeSourceId")) assertTrue(resources.has(field), field);
  }

  Yaml yaml() {
    return new Yaml(new StringDateConstructor());
  }

  /** OpenAPI dates are strings; prevent SnakeYAML's YAML 1.1 timestamp coercion. */
  static class StringDateConstructor extends SafeConstructor {
    StringDateConstructor() {
      super(new LoaderOptions());
      yamlConstructors.put(
          Tag.TIMESTAMP,
          new AbstractConstruct() {
            @Override
            public Object construct(Node node) {
              return ((ScalarNode) node).getValue();
            }
          });
    }
  }

  void assertDocumentsEqual(JsonNode expected, JsonNode actual, String location) {
    if (expected.isObject() && actual.isObject()) {
      assertEquals(expected.size(), actual.size(), location);
      expected
          .properties()
          .forEach(
              e -> {
                assertTrue(actual.has(e.getKey()), location + "/" + e.getKey());
                assertDocumentsEqual(
                    e.getValue(), actual.get(e.getKey()), location + "/" + e.getKey());
              });
    } else if (expected.isArray() && actual.isArray()) {
      assertEquals(expected.size(), actual.size(), location);
      for (int i = 0; i < expected.size(); i++)
        assertDocumentsEqual(expected.get(i), actual.get(i), location + "/" + i);
    } else if (expected.isNumber() && actual.isNumber()) {
      assertEquals(0, expected.decimalValue().compareTo(actual.decimalValue()), location);
    } else assertEquals(expected, actual, location);
  }

  void validateReferences(JsonNode node, JsonNode root) {
    if (node.isObject()) {
      if (node.has("$ref")) {
        String ref = node.get("$ref").asString();
        assertTrue(ref.startsWith("#/"), ref);
        assertFalse(root.at(ref.substring(1)).isMissingNode(), ref);
      }
      node.properties().forEach(e -> validateReferences(e.getValue(), root));
    } else if (node.isArray()) node.forEach(child -> validateReferences(child, root));
  }

  @Test
  void swaggerAndSecurityUseTheRealFilterChain() throws Exception {
    assertEquals(302, get("/swagger-ui.html", false).statusCode());
    assertEquals(200, get("/swagger-ui/index.html", false).statusCode());
    assertEquals(200, get("/v3/api-docs/swagger-config", false).statusCode());
    var denied = get("/api/v1/me", false);
    assertEquals(401, denied.statusCode());
    assertEquals("UNAUTHORIZED", mapper.readTree(denied.body()).get("error").asString());
    assertTrue(denied.headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Basic"));
    var identity = get("/api/v1/me", true);
    assertEquals(200, identity.statusCode());
    assertEquals("olda", mapper.readTree(identity.body()).get("username").asString());
  }

  @Test
  void requestExamplesMatchSchemasAndRealJakartaConstraints() throws Exception {
    var spec = mapper.readTree(get("/v3/api-docs", false).body());
    try (var factory = Validation.buildDefaultValidatorFactory();
        var files = Files.list(Path.of("docs/examples/requests"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
        String name = file.getFileName().toString().replace(".json", "");
        Class<?> type =
            Class.forName("dev.ioza.bank.infrastructure.adapters.in.controllers.request." + name);
        Object value = mapper.readValue(Files.readString(file), type);
        assertTrue(factory.getValidator().validate(value).isEmpty(), name);
        var example = spec.at("/components/schemas/" + name + "/example");
        // Funding and BudgetLine occur as nested schemas as well as standalone examples.
        assertFalse(example.isMissingNode(), name);
        assertDocumentsEqual(mapper.readTree(Files.readString(file)), example, name);
      }
    }
    assertEquals(
        "number",
        spec.at("/components/schemas/MovementResponse/properties/amount/type").asString());
    assertEquals(
        "decimal",
        spec.at("/components/schemas/MovementResponse/properties/amount/format").asString());
    var expenseSchema = spec.at("/components/schemas/ExpenseRequest");
    assertFalse(expenseSchema.get("required").toString().contains("fundingSources"));
    assertTrue(expenseSchema.at("/properties/fundingSources/nullable").asBoolean());
  }

  @Test
  void unexpectedErrorsAreJsonAndHttpErrorsRetainTheirStatus() throws Exception {
    var failed = get("/api/v1/households", true);
    assertEquals(500, failed.statusCode());
    assertEquals("INTERNAL_SERVER_ERROR", mapper.readTree(failed.body()).get("error").asString());
    assertFalse(failed.body().contains("UnsupportedOperationException"));
    assertEquals(404, get("/api/v1/no-such-resource", true).statusCode());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration(
      excludeName = "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration")
  @Import({
    OpenApiConfig.class,
    SecurityConfig.class,
    ApiExceptionHandler.class,
    HouseholdController.class,
    AccountController.class,
    EnvelopeController.class,
    BudgetController.class,
    DashboardController.class,
    MovementController.class
  })
  static class DocumentationApplication {
    static <T> T unavailable(Class<T> type) {
      return type.cast(
          Proxy.newProxyInstance(
              type.getClassLoader(),
              new Class<?>[] {type},
              (proxy, method, args) -> {
                if (method.getName().equals("toString"))
                  return "DocumentationPort:" + type.getSimpleName();
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                throw new UnsupportedOperationException(
                    "No financial execution in documentation tests");
              }));
    }

    @Bean
    Setup setup() {
      return unavailable(Setup.class);
    }

    @Bean
    Budget budget() {
      return unavailable(Budget.class);
    }

    @Bean
    Finance finance() {
      return unavailable(Finance.class);
    }

    @Bean
    Dashboard dashboard() {
      return unavailable(Dashboard.class);
    }

    @Bean
    IdempotentMovements idempotency() {
      return unavailable(IdempotentMovements.class);
    }
  }
}

package dev.ioza.bank.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ioza.bank.application.commands.CreateAccount;
import dev.ioza.bank.application.commands.CreateEnvelope;
import dev.ioza.bank.application.ports.in.Finance;
import dev.ioza.bank.application.ports.in.IdempotentMovements;
import dev.ioza.bank.application.queries.MovementQuery;
import dev.ioza.bank.domain.enums.AccountType;
import dev.ioza.bank.domain.enums.EnvelopeType;
import dev.ioza.bank.domain.enums.MovementStatus;
import dev.ioza.bank.domain.enums.MovementType;
import dev.ioza.bank.domain.enums.PeriodStatus;
import dev.ioza.bank.domain.model.BudgetPeriod;
import dev.ioza.bank.domain.model.Movement;
import dev.ioza.bank.infrastructure.adapters.in.controllers.AccountController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.BudgetController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.HouseholdController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.MovementController;
import dev.ioza.bank.infrastructure.adapters.in.controllers.advice.ApiExceptionHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HttpContractTest {
  @Test
  void catalogsKeepJsonFieldsAndValidateRequestsBeforeBusinessCode() throws Exception {
    var fixture = new SetupBudgetServiceTest();
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(
                new HouseholdController(fixture.setup, () -> "olda"),
                new AccountController(fixture.setup))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    mvc.perform(
            post("/api/v1/households")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Familia\",\"displayName\":\"Olda\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("MXN"));
    mvc.perform(
            post("/api/v1/households")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"displayName\":\"Olda\"}"))
        .andExpect(status().isBadRequest());
    String household = fixture.householdData.keySet().iterator().next();
    mvc.perform(
            post("/api/v1/households/" + household + "/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Banco\",\"type\":\"DIGITAL\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(0))
        .andExpect(jsonPath("$.householdId").value(household));
    assertEquals(1, fixture.accountData.size());
  }

  @Test
  void financialHeadersValidationAndPaginationReachTheInputPorts() throws Exception {
    AtomicReference<MovementQuery> query = new AtomicReference<>();
    AtomicReference<String> key = new AtomicReference<>();
    Movement result =
        new Movement(
            "m",
            "h",
            "q",
            MovementType.EXPENSE,
            MovementStatus.POSTED,
            new BigDecimal("50.00"),
            "Compra",
            "e",
            null,
            "olda",
            Instant.EPOCH,
            null,
            List.of(),
            List.of(),
            List.of());
    Finance finance =
        (Finance)
            Proxy.newProxyInstance(
                Finance.class.getClassLoader(),
                new Class<?>[] {Finance.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("movements")) {
                    query.set((MovementQuery) args[1]);
                    return List.of();
                  }
                  if (method.getName().equals("expense")) return result;
                  throw new AssertionError("Operación inesperada: " + method.getName());
                });
    IdempotentMovements idempotency =
        (h, k, operation, command, action) -> {
          key.set(k);
          return action.get();
        };
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new MovementController(finance, idempotency))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    String endpoint = "/api/v1/households/h/movements";
    String json =
        "{\"periodId\":\"q\",\"targetEnvelopeId\":\"e\",\"amount\":50.00,\"description\":\"Compra\"}";
    mvc.perform(post(endpoint + "/expenses").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post(endpoint + "/expenses")
                .header("Idempotency-Key", "purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(50.00))
        .andExpect(jsonPath("$.targetEnvelopeId").value("e"));
    assertEquals("purchase", key.get());
    mvc.perform(
            post(endpoint + "/expenses")
                .header("Idempotency-Key", "invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.replace("50.00", "50.001")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get(endpoint)
                .param("periodId", "q")
                .param("accountId", "a")
                .param("envelopeId", "e")
                .param("page", "2")
                .param("size", "10"))
        .andExpect(status().isOk());
    assertEquals(new MovementQuery("q", "a", "e", 2, 10), query.get());
    mvc.perform(get(endpoint).param("size", "101")).andExpect(status().isBadRequest());
    mvc.perform(get(endpoint).param("page", "wrong")).andExpect(status().isBadRequest());
  }

  @Test
  void periodPlanPutValidatesAmountAndReturnsUpdatedPeriod() throws Exception {
    var f = new SetupBudgetServiceTest();
    String h = f.household();
    var a = f.setup.createAccount(h, new CreateAccount("Banco", AccountType.DIGITAL));
    var e =
        f.setup.createEnvelope(
            h, new CreateEnvelope("Nuevo", a.id(), EnvelopeType.SAVINGS, "Ahorro"));
    f.periodData.put(
        "p",
        new BudgetPeriod(
            "p",
            h,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 14),
            PeriodStatus.OPEN,
            BigDecimal.ZERO,
            List.of()));
    var mvc =
        MockMvcBuilders.standaloneSetup(new BudgetController(f.budget))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    String url = "/api/v1/households/" + h + "/budget-periods/p/lines/" + e.id();
    for (String body :
        List.of(
            "{}",
            "{\"amount\":-1}",
            "{\"amount\":1.001}",
            "{\"amount\":null}",
            "{\"amount\":100000000000000000000000000000000}"))
      mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    for (int i = 0; i < 2; i++)
      mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1500}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value("p"))
          .andExpect(jsonPath("$.lines.length()").value(1))
          .andExpect(jsonPath("$.lines[0].envelopeId").value(e.id()))
          .andExpect(jsonPath("$.lines[0].amount").value(1500));
    f.budget.close(h, "p");
    mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}"))
        .andExpect(status().isConflict());
  }
}

package com.mckevin.agentcrm.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mckevin.agentcrm.application.port.CrmGateway;
import com.mckevin.agentcrm.domain.customer.Claim;
import com.mckevin.agentcrm.domain.customer.ClaimStatus;
import com.mckevin.agentcrm.domain.customer.Contract;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.customer.Segment;
import com.mckevin.agentcrm.support.InMemoryAdapters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Application complète (sécurité, cas d'usage, JPA, Flyway) sur PostgreSQL, avec les données de démonstration.
 * Le CRM est remplacé par un adaptateur en mémoire, ce que permet le port CrmGateway.
 */
@SpringBootTest(properties = "app.demo.enabled=true")
@AutoConfigureMockMvc
class AgencyApiIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Paris"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class CrmStub {
        @Bean
        @Primary
        InMemoryAdapters.Crm crmGateway() {
            InMemoryAdapters.Crm crm = new InMemoryAdapters.Crm();
            crm.put(new CustomerProfile(customer("C-10234", "Claire", "Martin"), List.of(
                    new Contract("AUT-558201", ProductLine.AUTO, "Auto Confort", ContractStatus.ACTIVE, new BigDecimal("712.40"),
                            TODAY.minusYears(4), TODAY.plusDays(12))), List.of()));
            crm.put(new CustomerProfile(customer("C-10235", "Antoine", "Dubois"), List.of(), List.of(
                    new Claim("SIN-77245", "MRH-114502", "Dégât des eaux", ClaimStatus.IN_PROGRESS, TODAY.minusDays(47),
                            new BigDecimal("2350")))));
            crm.put(new CustomerProfile(customer("C-10242", "Léa", "Girard"), List.of(), List.of()));
            return crm;
        }

        private static Customer customer(String id, String first, String last) {
            return new Customer(new CustomerId(id), "Mme", first, last, LocalDate.of(1986, 4, 12), null, null, "Lille", "59000",
                    Segment.INDIVIDUAL, LocalDate.of(2014, 3, 1));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private CrmGateway crm;

    @AfterEach
    void crmBackOnline() {
        ((InMemoryAdapters.Crm) crm).setDown(false);
    }

    private String login(String email) throws Exception {
        JsonNode body = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "demo1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private ResultActions as(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    @Test
    void authentication() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mckevin@demo.fr\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        as(login("mckevin@demo.fr"), get("/api/auth/me"))
                .andExpect(jsonPath("$.fullName").value("Mckevin Kamguem"))
                .andExpect(jsonPath("$.manager").value(false));
    }

    @Test
    void advisorSeesOnlyHisPortfolio() throws Exception {
        String token = login("mckevin@demo.fr");
        as(token, get("/api/customers").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(true))
                .andExpect(jsonPath("$.customers[*].customer.id", hasItem("C-10234")))
                .andExpect(jsonPath("$.customers[*].customer.id", not(hasItem("C-10242"))));
        as(token, get("/api/customers/C-10242"))
                .andExpect(status().isForbidden());
        as(token, get("/api/customers/INVALIDE")).andExpect(status().isBadRequest());
    }

    @Test
    void customerViewAndRecommendationWorkflow() throws Exception {
        String token = login("mckevin@demo.fr");
        as(token, get("/api/customers/C-10234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.fullName").value("Claire Martin"))
                .andExpect(jsonPath("$.advisorName").value("Mckevin Kamguem"))
                .andExpect(jsonPath("$.kpis.activeContracts").value(1))
                .andExpect(jsonPath("$.recommendations[0].code").value("RENEWAL_SOON"))
                .andExpect(jsonPath("$.recommendations[0].priority").value("HIGH"))
                // L'opportunité habitation a déjà une tâche ouverte dans les données de démonstration
                .andExpect(jsonPath("$.recommendations[*].code", not(hasItem("HOME_CROSS_SELL"))))
                .andExpect(jsonPath("$.interactions.length()").value(1));

        as(token, post("/api/customers/C-10234/recommendations/RENEWAL_SOON/accept")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"AUT-558201\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.recommendationCode").value("RENEWAL_SOON"));

        as(token, get("/api/customers/C-10234"))
                .andExpect(jsonPath("$.recommendations[*].code", not(hasItem("RENEWAL_SOON"))))
                .andExpect(jsonPath("$.openTasks[*].title", hasItem("Préparer le renouvellement – Auto AUT-558201")));

        as(token, post("/api/customers/C-10234/interactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"PHONE\",\"direction\":\"OUTBOUND\",\"subject\":\"Proposition de renouvellement\","
                        + "\"followUpOn\":\"" + TODAY.plusDays(5) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentName").value("Mckevin Kamguem"));

        as(token, get("/api/tasks").param("openOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Relance : Proposition de renouvellement")));
    }

    @Test
    void dismissRequiresAReason() throws Exception {
        String token = login("mckevin@demo.fr");
        as(token, post("/api/customers/C-10235/recommendations/STALE_CLAIM/dismiss").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"SIN-77245\",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
        as(token, post("/api/customers/C-10235/recommendations/STALE_CLAIM/dismiss").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"SIN-77245\",\"reason\":\"Expertise programmée la semaine prochaine\"}"))
                .andExpect(status().isNoContent());
        as(token, get("/api/customers/C-10235"))
                .andExpect(jsonPath("$.recommendations[*].code", not(hasItem("STALE_CLAIM"))));
    }

    @Test
    void degradedModeServesTheLastKnownCopy() throws Exception {
        String token = login("mckevin@demo.fr");
        as(token, get("/api/customers/C-10234")).andExpect(jsonPath("$.live").value(true));

        ((InMemoryAdapters.Crm) crm).setDown(true);

        as(token, get("/api/customers/C-10234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.customer.fullName").value("Claire Martin"));
        as(token, get("/api/customers/C-10237"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CRM_UNAVAILABLE"));
        as(token, get("/api/customers").param("q", "martin"))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.customers[0].customer.id").value("C-10234"));
        as(token, get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false));
    }

    @Test
    void managerViews() throws Exception {
        String manager = login("sophie@demo.fr");
        JsonNode team = json.readTree(as(manager, get("/api/team")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(team).hasSize(2);
        assertThat(team.findValuesAsText("fullName")).contains("Mckevin Kamguem", "Lucas Morel");

        as(manager, get("/api/customers/C-10242")).andExpect(status().isOk());
        as(login("lucas@demo.fr"), get("/api/team")).andExpect(status().isForbidden());
        as(manager, get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers").value(14));
        as(manager, get("/api/crm/status")).andExpect(status().isOk());
    }

    @Test
    void tasksCanBeRescheduledAndCompleted() throws Exception {
        String token = login("mckevin@demo.fr");
        JsonNode tasks = json.readTree(as(token, get("/api/tasks").param("openOnly", "true"))
                .andReturn().getResponse().getContentAsString());
        JsonNode overdue = null;
        for (JsonNode t : tasks) {
            if (t.get("overdue").asBoolean()) {
                overdue = t;
            }
        }
        assertThat(overdue).as("une tâche en retard dans les données de démonstration").isNotNull();
        long id = overdue.get("id").asLong();

        as(token, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tasks/{id}/due-date", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dueDate\":\"" + TODAY.minusDays(1) + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUE_DATE_IN_PAST"));
        as(token, post("/api/tasks/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        as(login("lucas@demo.fr"), post("/api/tasks/{id}/complete", id)).andExpect(status().isForbidden());
    }
}

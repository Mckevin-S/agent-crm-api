package com.mckevin.agentcrm.adapter.out.crm;

import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.domain.customer.ClaimStatus;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.customer.Segment;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Adaptateur CRM : traduction du vocabulaire du CRM et comportement en cas de panne. */
class CrmHttpAdapterTest {

    private static final String BASE = "http://crm.test";
    private MockRestServiceServer server;
    private CrmHttpAdapter adapter;
    private CircuitBreakerRegistry breakers;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        breakers = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(4).minimumNumberOfCalls(2).failureRateThreshold(50)
                .recordExceptions(HttpServerErrorException.class).build());
        RetryRegistry retries = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
        adapter = new CrmHttpAdapter(builder.build(), breakers, retries, Clock.systemUTC());
    }

    @Test
    void translatesCrmVocabularyIntoTheDomain() {
        server.expect(requestTo(BASE + "/crm/v1/clients/C-10234")).andRespond(withSuccess("""
                {"num_client":"C-10234","civilite":"Mme","nom":"Martin","prenom":"Claire","date_naissance":"1986-04-12",
                 "ville":"Lille","code_postal":"59000","segment":"PART","client_depuis":"2014-03-01","champ_inconnu":1}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/crm/v1/clients/C-10234/contrats")).andRespond(withSuccess("""
                [{"num_contrat":"MRH-1","produit":"MRH","libelle":"Habitation","statut":"SUSPENDU","prime_annuelle":312.5,
                  "date_effet":"2020-01-01","date_echeance":"2027-01-01"}]
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/crm/v1/clients/C-10234/sinistres")).andRespond(withSuccess("""
                [{"num_sinistre":"SIN-1","num_contrat":"MRH-1","nature":"Dégât des eaux","statut":"EN_COURS",
                  "date_ouverture":"2026-10-01","montant_estime":1800}]
                """, MediaType.APPLICATION_JSON));

        CustomerProfile profile = adapter.fetchProfile(new CustomerId("C-10234")).orElseThrow();

        assertThat(profile.customer().fullName()).isEqualTo("Claire Martin");
        assertThat(profile.customer().segment()).isEqualTo(Segment.INDIVIDUAL);
        assertThat(profile.contracts()).singleElement().satisfies(c -> {
            assertThat(c.product()).isEqualTo(ProductLine.HOME);
            assertThat(c.status()).isEqualTo(ContractStatus.SUSPENDED);
        });
        assertThat(profile.claims().get(0).status()).isEqualTo(ClaimStatus.IN_PROGRESS);
        assertThat(adapter.status().available()).isTrue();
    }

    @Test
    void unknownCustomerIsEmpty() {
        server.expect(requestTo(BASE + "/crm/v1/clients/C-99999")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.fetchProfile(new CustomerId("C-99999"))).isEqualTo(Optional.empty());
    }

    @Test
    void repeatedFailuresOpenTheCircuit() {
        server.expect(ExpectedCount.times(2), requestTo(BASE + "/crm/v1/clients?q=")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.search("")).isInstanceOf(CrmUnavailableException.class);
        assertThatThrownBy(() -> adapter.search("")).isInstanceOf(CrmUnavailableException.class);
        // Circuit ouvert : l'appel échoue immédiatement, sans requête HTTP
        assertThatThrownBy(() -> adapter.search("")).isInstanceOf(CrmUnavailableException.class)
                .hasMessageContaining("circuit ouvert");

        assertThat(adapter.status().available()).isFalse();
        assertThat(adapter.status().circuitState()).isEqualTo("OPEN");
        server.verify();
    }

    @Test
    void resettingTheCircuitMakesTheCrmAvailableAgain() {
        server.expect(ExpectedCount.times(2), requestTo(BASE + "/crm/v1/clients?q=")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> adapter.search("")).isInstanceOf(CrmUnavailableException.class);
        assertThatThrownBy(() -> adapter.search("")).isInstanceOf(CrmUnavailableException.class);
        assertThat(adapter.status().available()).isFalse();

        breakers.circuitBreaker(CrmHttpAdapter.RESILIENCE_NAME).reset();

        assertThat(adapter.status().available()).isTrue();
        assertThat(adapter.status().lastFailureAt()).isNull();
        assertThat(adapter.status().circuitState()).isEqualTo("CLOSED");
    }

    @Test
    void unknownProductCodeIsRejected() {
        assertThatThrownBy(() -> CrmTranslator.product("XYZ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(CrmTranslator.product("PRV")).isEqualTo(ProductLine.PROTECTION);
    }
}

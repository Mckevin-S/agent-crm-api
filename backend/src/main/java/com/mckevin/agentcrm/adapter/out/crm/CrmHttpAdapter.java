package com.mckevin.agentcrm.adapter.out.crm;

import com.mckevin.agentcrm.application.port.CrmGateway;
import com.mckevin.agentcrm.application.port.CrmStatusProvider;
import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Adaptateur HTTP vers le CRM. Chaque appel passe par un disjoncteur et une nouvelle tentative (Resilience4j) :
 * en cas de panne répétée, le circuit s'ouvre et l'application bascule immédiatement en mode dégradé
 * au lieu d'attendre des délais d'expiration.
 */
@Component
class CrmHttpAdapter implements CrmGateway, CrmStatusProvider {

    static final String RESILIENCE_NAME = "crm";

    private static final ParameterizedTypeReference<List<CrmDtos.ClientDto>> CLIENTS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<CrmDtos.ContratDto>> CONTRATS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<CrmDtos.SinistreDto>> SINISTRES = new ParameterizedTypeReference<>() { };

    private final RestClient client;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Clock clock;
    private final AtomicReference<Instant> lastSuccess = new AtomicReference<>();
    private final AtomicReference<Instant> lastFailure = new AtomicReference<>();

    CrmHttpAdapter(RestClient crmRestClient, CircuitBreakerRegistry circuitBreakers, RetryRegistry retries, Clock clock) {
        this.client = crmRestClient;
        this.circuitBreaker = circuitBreakers.circuitBreaker(RESILIENCE_NAME);
        this.retry = retries.retry(RESILIENCE_NAME);
        this.clock = clock;
    }

    @Override
    public List<Customer> search(String query) {
        List<CrmDtos.ClientDto> found = call(() -> client.get()
                .uri(b -> b.path("/crm/v1/clients").queryParam("q", query == null ? "" : query).build())
                .retrieve().body(CLIENTS));
        return found == null ? List.of() : found.stream().map(CrmTranslator::customer).toList();
    }

    @Override
    public Optional<CustomerProfile> fetchProfile(CustomerId id) {
        return call(() -> {
            CrmDtos.ClientDto dto;
            try {
                dto = client.get().uri("/crm/v1/clients/{id}", id.value()).retrieve().body(CrmDtos.ClientDto.class);
            } catch (HttpClientErrorException.NotFound e) {
                return Optional.<CustomerProfile>empty();
            }
            List<CrmDtos.ContratDto> contrats = client.get().uri("/crm/v1/clients/{id}/contrats", id.value())
                    .retrieve().body(CONTRATS);
            List<CrmDtos.SinistreDto> sinistres = client.get().uri("/crm/v1/clients/{id}/sinistres", id.value())
                    .retrieve().body(SINISTRES);
            return Optional.of(new CustomerProfile(CrmTranslator.customer(dto),
                    contrats == null ? List.of() : contrats.stream().map(CrmTranslator::contract).toList(),
                    sinistres == null ? List.of() : sinistres.stream().map(CrmTranslator::claim).toList()));
        });
    }

    @Override
    public CrmStatus status() {
        CircuitBreaker.State state = circuitBreaker.getState();
        boolean available = state != CircuitBreaker.State.OPEN && state != CircuitBreaker.State.FORCED_OPEN
                && (lastFailure.get() == null || lastSuccess.get() != null && lastSuccess.get().isAfter(lastFailure.get()));
        return new CrmStatus(available, state.name(), lastSuccess.get(), lastFailure.get());
    }

    private <T> T call(Supplier<T> supplier) {
        Supplier<T> guarded = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, supplier));
        try {
            T result = guarded.get();
            lastSuccess.set(Instant.now(clock));
            return result;
        } catch (CallNotPermittedException e) {
            lastFailure.set(Instant.now(clock));
            throw new CrmUnavailableException("CRM indisponible (circuit ouvert)", e);
        } catch (RestClientException e) {
            lastFailure.set(Instant.now(clock));
            throw new CrmUnavailableException("CRM indisponible : " + e.getMessage(), e);
        }
    }
}

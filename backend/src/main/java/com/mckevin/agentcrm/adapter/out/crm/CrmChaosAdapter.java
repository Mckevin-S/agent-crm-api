package com.mckevin.agentcrm.adapter.out.crm;

import com.mckevin.agentcrm.application.port.CrmFaultInjector;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outil de démonstration : pilote l'injection de pannes du CRM simulé. Au retour à la normale,
 * le disjoncteur est refermé pour que la reprise soit visible immédiatement.
 */
@Component
class CrmChaosAdapter implements CrmFaultInjector {

    private final RestClient client;
    private final CircuitBreakerRegistry circuitBreakers;

    CrmChaosAdapter(RestClient crmRestClient, CircuitBreakerRegistry circuitBreakers) {
        this.client = crmRestClient;
        this.circuitBreakers = circuitBreakers;
    }

    @Override
    public void simulateOutage(boolean outage) {
        client.put().uri("/admin/chaos").body(new CrmDtos.ChaosDto(outage ? 1 : 0, 0)).retrieve().toBodilessEntity();
        if (!outage) {
            circuitBreakers.circuitBreaker(CrmHttpAdapter.RESILIENCE_NAME).reset();
        }
    }

    @Override
    public boolean outageSimulated() {
        try {
            CrmDtos.ChaosDto chaos = client.get().uri("/admin/chaos").retrieve().body(CrmDtos.ChaosDto.class);
            return chaos != null && chaos.failureRate() >= 1;
        } catch (RestClientException e) {
            return false;
        }
    }
}

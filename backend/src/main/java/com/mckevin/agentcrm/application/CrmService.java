package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.port.CrmFaultInjector;
import com.mckevin.agentcrm.application.port.CrmStatusProvider;
import com.mckevin.agentcrm.application.port.CrmStatusProvider.CrmStatus;

/** État du CRM et outil de démonstration du mode dégradé (réservé aux managers). */
public class CrmService {

    public record CrmState(CrmStatus status, boolean outageSimulated) {
    }

    private final CrmStatusProvider statusProvider;
    private final CrmFaultInjector faultInjector;

    public CrmService(CrmStatusProvider statusProvider, CrmFaultInjector faultInjector) {
        this.statusProvider = statusProvider;
        this.faultInjector = faultInjector;
    }

    public CrmState state() {
        return new CrmState(statusProvider.status(), faultInjector.outageSimulated());
    }

    public CrmState simulateOutage(Agent agent, boolean outage) {
        if (!agent.manager()) {
            throw new AccessDeniedException("La simulation de panne est réservée aux managers.");
        }
        faultInjector.simulateOutage(outage);
        return state();
    }
}

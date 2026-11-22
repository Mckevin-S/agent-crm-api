package com.mckevin.agentcrm.application.port;

/** Outil de démonstration : simule une panne du CRM pour montrer le mode dégradé. */
public interface CrmFaultInjector {

    void simulateOutage(boolean outage);

    boolean outageSimulated();
}

package com.mckevin.agentcrm.application.port;

import java.time.Instant;

/** État de la connexion au CRM, pour l'affichage et la supervision. */
public interface CrmStatusProvider {

    record CrmStatus(boolean available, String circuitState, Instant lastSuccessAt, Instant lastFailureAt) {
    }

    CrmStatus status();
}

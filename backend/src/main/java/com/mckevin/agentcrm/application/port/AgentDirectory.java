package com.mckevin.agentcrm.application.port;

import java.util.List;
import java.util.Optional;

/** Annuaire des conseillers (comptes de l'application). */
public interface AgentDirectory {

    record AgentSummary(long id, String fullName, String email, boolean manager) {
    }

    List<AgentSummary> agents();

    Optional<AgentSummary> find(long id);
}

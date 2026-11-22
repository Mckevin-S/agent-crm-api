package com.mckevin.agentcrm.application.port;

import java.util.Optional;

/** Accès aux comptes pour l'authentification. */
public interface AgentAccounts {

    record Credentials(long id, String fullName, String email, String passwordHash, boolean manager) {
    }

    Optional<Credentials> findByEmail(String email);
}

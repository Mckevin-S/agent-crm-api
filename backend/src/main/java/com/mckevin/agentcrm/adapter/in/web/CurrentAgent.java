package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.application.Agent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Construit l'utilisateur métier à partir du jeton JWT de la requête. */
@Component
public class CurrentAgent {

    public Agent get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return new Agent(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString("name"),
                    "MANAGER".equals(jwt.getClaimAsString("role")));
        }
        throw new IllegalStateException("Aucun utilisateur authentifié");
    }
}

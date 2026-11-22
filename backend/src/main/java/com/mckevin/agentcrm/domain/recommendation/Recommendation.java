package com.mckevin.agentcrm.domain.recommendation;

import com.mckevin.agentcrm.domain.shared.Priority;

import java.time.LocalDate;

/**
 * Action conseillée sur un client. Elle est identifiée par son code et sa référence (contrat, sinistre,
 * ou vide pour une action sur le client), ce qui permet de l'écarter ou de la transformer en tâche.
 */
public record Recommendation(RecommendationCode code, String reference, Priority priority, String title, String rationale,
                             LocalDate suggestedDueDate) {

    public Recommendation {
        reference = reference == null ? "" : reference;
    }

    public boolean matches(RecommendationCode otherCode, String otherReference) {
        return code == otherCode && reference.equals(otherReference == null ? "" : otherReference);
    }
}

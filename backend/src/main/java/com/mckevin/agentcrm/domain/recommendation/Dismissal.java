package com.mckevin.agentcrm.domain.recommendation;

import com.mckevin.agentcrm.domain.customer.CustomerId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Recommandation écartée par un conseiller : elle ne réapparaît pas pendant 90 jours. */
public record Dismissal(CustomerId customerId, RecommendationCode code, String reference, long agentId, String reason,
                        Instant dismissedAt) {

    public static final int SNOOZE_DAYS = 90;

    public Dismissal {
        reference = reference == null ? "" : reference;
    }

    public boolean isActiveAt(Instant now) {
        return dismissedAt.plus(SNOOZE_DAYS, ChronoUnit.DAYS).isAfter(now);
    }
}

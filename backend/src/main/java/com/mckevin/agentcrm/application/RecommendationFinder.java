package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.port.DismissalRepository;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.recommendation.CustomerSituation;
import com.mckevin.agentcrm.domain.recommendation.NextBestActionEngine;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Assemble la situation d'un client (profil, dernier échange, tâches, refus) et interroge le moteur de règles. */
public class RecommendationFinder {

    private final InteractionRepository interactions;
    private final TaskRepository tasks;
    private final DismissalRepository dismissals;
    private final NextBestActionEngine engine;
    private final Clock clock;

    public RecommendationFinder(InteractionRepository interactions, TaskRepository tasks, DismissalRepository dismissals,
                                NextBestActionEngine engine, Clock clock) {
        this.interactions = interactions;
        this.tasks = tasks;
        this.dismissals = dismissals;
        this.engine = engine;
        this.clock = clock;
    }

    public List<Recommendation> recommendationsFor(CustomerProfile profile) {
        return recommendationsFor(profile, interactions.lastInteraction(profile.customer().id()));
    }

    public List<Recommendation> recommendationsFor(CustomerProfile profile, Optional<Instant> lastInteraction) {
        var id = profile.customer().id();
        return engine.recommend(new CustomerSituation(profile, lastInteraction, LocalDate.now(clock)),
                dismissals.findByCustomer(id), tasks.findByCustomer(id), Instant.now(clock));
    }
}

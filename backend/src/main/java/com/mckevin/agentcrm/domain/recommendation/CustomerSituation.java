package com.mckevin.agentcrm.domain.recommendation;

import com.mckevin.agentcrm.domain.customer.CustomerProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/** Données nécessaires aux règles : profil CRM, dernier échange connu et date du jour. */
public record CustomerSituation(CustomerProfile profile, Optional<Instant> lastInteraction, LocalDate today) {
}

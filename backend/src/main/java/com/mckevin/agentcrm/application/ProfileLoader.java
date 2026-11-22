package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.port.CrmGateway;
import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.application.port.ProfileSnapshots;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Charge un profil depuis le CRM et en garde une copie. Si le CRM est indisponible, renvoie la dernière
 * copie connue, marquée comme non à jour : c'est le mode dégradé.
 */
public class ProfileLoader {

    /** Profil chargé, avec sa fraîcheur. */
    public record LoadedProfile(CustomerProfile profile, boolean live, Instant fetchedAt) {
    }

    private final CrmGateway crm;
    private final ProfileSnapshots snapshots;
    private final Clock clock;

    public ProfileLoader(CrmGateway crm, ProfileSnapshots snapshots, Clock clock) {
        this.crm = crm;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    /** @throws NotFoundException si le client n'existe pas ; CrmUnavailableException si le CRM est en panne sans copie locale */
    public LoadedProfile load(CustomerId id) {
        try {
            CustomerProfile profile = crm.fetchProfile(id)
                    .orElseThrow(() -> new NotFoundException("Client introuvable : " + id));
            Instant now = Instant.now(clock);
            snapshots.save(profile, now);
            return new LoadedProfile(profile, true, now);
        } catch (CrmUnavailableException e) {
            return snapshots.find(id)
                    .map(s -> new LoadedProfile(s.profile(), false, s.fetchedAt()))
                    .orElseThrow(() -> e);
        }
    }

    /** Variante tolérante pour les vues agrégées : un client sans copie locale est simplement ignoré. */
    public Optional<LoadedProfile> tryLoad(CustomerId id) {
        try {
            return Optional.of(load(id));
        } catch (CrmUnavailableException | NotFoundException e) {
            return Optional.empty();
        }
    }
}

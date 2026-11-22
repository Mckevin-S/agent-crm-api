package com.mckevin.agentcrm.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mckevin.agentcrm.application.port.ProfileSnapshots;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Copies des profils CRM stockées en JSON, avec un texte normalisé pour la recherche en mode dégradé. */
@Component
class ProfileSnapshotAdapter implements ProfileSnapshots {

    private final SnapshotSpringRepository repository;
    private final ObjectMapper json;

    ProfileSnapshotAdapter(SnapshotSpringRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    public void save(CustomerProfile profile, Instant fetchedAt) {
        Customer c = profile.customer();
        String searchText = String.join(" ", c.id().value(), c.firstName(), c.lastName(), nullToEmpty(c.email()),
                nullToEmpty(c.city())).toLowerCase(Locale.ROOT);
        try {
            repository.save(new SnapshotEntity(c.id().value(), c.fullName(), searchText, json.writeValueAsString(profile),
                    fetchedAt));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation du profil impossible", e);
        }
    }

    @Override
    public Optional<Snapshot> find(CustomerId id) {
        return repository.findById(id.value()).map(e -> new Snapshot(read(e), e.getFetchedAt()));
    }

    @Override
    public List<Customer> search(String query) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        return repository.search(pattern).stream().map(e -> read(e).customer()).toList();
    }

    private CustomerProfile read(SnapshotEntity entity) {
        try {
            return json.readValue(entity.getPayload(), CustomerProfile.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Copie de profil illisible", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

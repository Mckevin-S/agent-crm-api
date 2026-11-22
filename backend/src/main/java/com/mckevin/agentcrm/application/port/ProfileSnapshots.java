package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Dernière copie connue des profils CRM, utilisée en mode dégradé. */
public interface ProfileSnapshots {

    record Snapshot(CustomerProfile profile, Instant fetchedAt) {
    }

    void save(CustomerProfile profile, Instant fetchedAt);

    Optional<Snapshot> find(CustomerId id);

    List<Customer> search(String query);
}

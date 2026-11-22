package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.interaction.Interaction;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InteractionRepository {

    Interaction save(Interaction interaction);

    List<Interaction> findByCustomer(CustomerId customerId);

    Optional<Instant> lastInteraction(CustomerId customerId);

    Map<CustomerId, Instant> lastInteractions(Collection<CustomerId> customerIds);

    long countByAgentSince(long agentId, Instant since);
}

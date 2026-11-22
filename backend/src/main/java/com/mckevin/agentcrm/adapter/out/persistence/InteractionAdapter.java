package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
class InteractionAdapter implements InteractionRepository {

    private final InteractionSpringRepository repository;

    InteractionAdapter(InteractionSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Interaction save(Interaction interaction) {
        return repository.save(InteractionEntity.from(interaction)).toDomain();
    }

    @Override
    public List<Interaction> findByCustomer(CustomerId customerId) {
        return repository.findByCustomerIdOrderByOccurredAtDesc(customerId.value()).stream()
                .map(InteractionEntity::toDomain).toList();
    }

    @Override
    public Optional<Instant> lastInteraction(CustomerId customerId) {
        return Optional.ofNullable(repository.lastOccurrence(customerId.value()));
    }

    @Override
    public Map<CustomerId, Instant> lastInteractions(Collection<CustomerId> customerIds) {
        Map<CustomerId, Instant> result = new HashMap<>();
        if (customerIds.isEmpty()) {
            return result;
        }
        for (Object[] row : repository.lastOccurrences(customerIds.stream().map(CustomerId::value).toList())) {
            result.put(new CustomerId((String) row[0]), (Instant) row[1]);
        }
        return result;
    }

    @Override
    public long countByAgentSince(long agentId, Instant since) {
        return repository.countByAgentIdAndOccurredAtAfter(agentId, since);
    }
}

package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.application.port.DismissalRepository;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class DismissalAdapter implements DismissalRepository {

    private final DismissalSpringRepository repository;

    DismissalAdapter(DismissalSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Dismissal dismissal) {
        repository.save(DismissalEntity.from(dismissal));
    }

    @Override
    public List<Dismissal> findByCustomer(CustomerId customerId) {
        return repository.findByCustomerId(customerId.value()).stream().map(DismissalEntity::toDomain).toList();
    }
}

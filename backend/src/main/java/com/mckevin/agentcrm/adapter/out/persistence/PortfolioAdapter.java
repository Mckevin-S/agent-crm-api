package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.application.port.PortfolioRepository;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class PortfolioAdapter implements PortfolioRepository {

    private final PortfolioSpringRepository repository;

    PortfolioAdapter(PortfolioSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CustomerId> customersOf(long agentId) {
        return repository.findByAgentIdOrderByCustomerId(agentId).stream().map(a -> new CustomerId(a.getCustomerId())).toList();
    }

    @Override
    public List<CustomerId> allCustomers() {
        return repository.findAll(Sort.by("customerId")).stream().map(a -> new CustomerId(a.getCustomerId())).toList();
    }

    @Override
    public Optional<Long> advisorOf(CustomerId customerId) {
        return repository.findById(customerId.value()).map(PortfolioAssignmentEntity::getAgentId);
    }
}

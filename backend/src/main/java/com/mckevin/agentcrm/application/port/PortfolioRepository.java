package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

/** Répartition des clients entre conseillers. */
public interface PortfolioRepository {

    List<CustomerId> customersOf(long agentId);

    List<CustomerId> allCustomers();

    Optional<Long> advisorOf(CustomerId customerId);
}

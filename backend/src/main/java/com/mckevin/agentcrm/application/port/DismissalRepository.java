package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;

import java.util.List;

public interface DismissalRepository {

    void save(Dismissal dismissal);

    List<Dismissal> findByCustomer(CustomerId customerId);
}

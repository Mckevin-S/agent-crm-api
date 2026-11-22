package com.mckevin.agentcrm.application.port;

import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;

import java.util.List;
import java.util.Optional;

/** Port vers le CRM externe, source de vérité des clients, contrats et sinistres. */
public interface CrmGateway {

    /** @throws CrmUnavailableException si le CRM ne répond pas */
    List<Customer> search(String query);

    /** @throws CrmUnavailableException si le CRM ne répond pas */
    Optional<CustomerProfile> fetchProfile(CustomerId id);
}

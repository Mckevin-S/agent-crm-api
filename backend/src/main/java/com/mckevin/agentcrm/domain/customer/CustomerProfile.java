package com.mckevin.agentcrm.domain.customer;

import java.math.BigDecimal;
import java.util.List;

/** Tout ce que le CRM sait d'un client : identité, contrats et sinistres. */
public record CustomerProfile(Customer customer, List<Contract> contracts, List<Claim> claims) {

    public CustomerProfile {
        contracts = List.copyOf(contracts);
        claims = List.copyOf(claims);
    }

    public List<Contract> activeContracts() {
        return contracts.stream().filter(Contract::isActive).toList();
    }

    public boolean holdsActive(ProductLine product) {
        return contracts.stream().anyMatch(c -> c.isActive() && c.product() == product);
    }

    public BigDecimal annualPremium() {
        return activeContracts().stream().map(Contract::annualPremium).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Claim> pendingClaims() {
        return claims.stream().filter(c -> c.status().isPending()).toList();
    }
}

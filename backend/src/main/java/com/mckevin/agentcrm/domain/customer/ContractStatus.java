package com.mckevin.agentcrm.domain.customer;

public enum ContractStatus {
    ACTIVE("En vigueur"),
    SUSPENDED("Suspendu"),
    TERMINATED("Résilié");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

package com.mckevin.agentcrm.domain.customer;

public enum ClaimStatus {
    OPEN("Ouvert"),
    IN_PROGRESS("En cours"),
    CLOSED("Clos"),
    REJECTED("Refusé");

    private final String label;

    ClaimStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isPending() {
        return this == OPEN || this == IN_PROGRESS;
    }
}

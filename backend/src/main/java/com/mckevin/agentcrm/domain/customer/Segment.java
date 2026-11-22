package com.mckevin.agentcrm.domain.customer;

public enum Segment {
    INDIVIDUAL("Particulier"),
    PROFESSIONAL("Professionnel");

    private final String label;

    Segment(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

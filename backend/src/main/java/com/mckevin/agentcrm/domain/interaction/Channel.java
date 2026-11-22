package com.mckevin.agentcrm.domain.interaction;

public enum Channel {
    PHONE("Téléphone"),
    EMAIL("E-mail"),
    MEETING("Rendez-vous"),
    VIDEO("Visio");

    private final String label;

    Channel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

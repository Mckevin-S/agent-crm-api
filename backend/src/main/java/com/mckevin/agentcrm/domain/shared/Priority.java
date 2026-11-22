package com.mckevin.agentcrm.domain.shared;

public enum Priority {
    LOW("Basse"),
    MEDIUM("Moyenne"),
    HIGH("Haute");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

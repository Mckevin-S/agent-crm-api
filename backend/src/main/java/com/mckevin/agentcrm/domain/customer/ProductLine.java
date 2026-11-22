package com.mckevin.agentcrm.domain.customer;

public enum ProductLine {
    AUTO("Auto"),
    HOME("Habitation"),
    HEALTH("Santé"),
    PROTECTION("Prévoyance"),
    SAVINGS("Épargne");

    private final String label;

    ProductLine(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

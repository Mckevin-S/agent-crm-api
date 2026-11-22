package com.mckevin.agentcrm.domain.shared;

/** Règle métier non respectée. Le code est stable (interprétable par un client), le message est destiné à l'utilisateur. */
public class BusinessRuleViolation extends RuntimeException {

    private final String code;

    public BusinessRuleViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

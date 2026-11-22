package com.mckevin.agentcrm.domain.customer;

import java.util.Objects;
import java.util.regex.Pattern;

/** Identifiant client tel que défini par le CRM (ex. C-10234). */
public record CustomerId(String value) {

    private static final Pattern FORMAT = Pattern.compile("C-\\d{5}");

    public CustomerId {
        Objects.requireNonNull(value, "identifiant client obligatoire");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Identifiant client invalide : " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

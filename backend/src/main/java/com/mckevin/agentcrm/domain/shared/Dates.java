package com.mckevin.agentcrm.domain.shared;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Formatage des dates dans les messages métier. */
public final class Dates {

    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Dates() {
    }

    public static String fr(LocalDate date) {
        return date == null ? "" : FR.format(date);
    }
}

package com.mckevin.agentcrm.domain.interaction;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.shared.BusinessRuleViolation;

import java.time.Instant;
import java.util.Objects;

/** Échange tracé entre un conseiller et un client. */
public record Interaction(Long id, CustomerId customerId, long agentId, String agentName, Channel channel,
                          Direction direction, String subject, String notes, Instant occurredAt) {

    public static final int MAX_SUBJECT = 120;
    public static final int MAX_NOTES = 2000;

    public Interaction {
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(occurredAt);
        if (subject == null || subject.isBlank() || subject.length() > MAX_SUBJECT) {
            throw new BusinessRuleViolation("INVALID_SUBJECT", "L'objet de l'échange est obligatoire (120 caractères au plus).");
        }
        if (notes != null && notes.length() > MAX_NOTES) {
            throw new BusinessRuleViolation("NOTES_TOO_LONG", "Les notes sont limitées à 2000 caractères.");
        }
        subject = subject.trim();
    }

    /** Un échange ne peut pas être daté dans le futur (tolérance de 5 minutes pour les horloges). */
    public static Interaction log(CustomerId customerId, long agentId, String agentName, Channel channel, Direction direction,
                                  String subject, String notes, Instant occurredAt, Instant now) {
        if (occurredAt.isAfter(now.plusSeconds(300))) {
            throw new BusinessRuleViolation("INTERACTION_IN_FUTURE", "Un échange ne peut pas être daté dans le futur.");
        }
        return new Interaction(null, customerId, agentId, agentName, channel, direction, subject, notes, occurredAt);
    }
}

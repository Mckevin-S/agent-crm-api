package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.interaction.Channel;
import com.mckevin.agentcrm.domain.interaction.Direction;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "interaction")
public class InteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    @Column(name = "agent_id", nullable = false)
    private long agentId;

    @Column(name = "agent_name", nullable = false, length = 80)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(length = 2000)
    private String notes;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected InteractionEntity() {
    }

    static InteractionEntity from(Interaction i) {
        InteractionEntity e = new InteractionEntity();
        e.id = i.id();
        e.customerId = i.customerId().value();
        e.agentId = i.agentId();
        e.agentName = i.agentName();
        e.channel = i.channel();
        e.direction = i.direction();
        e.subject = i.subject();
        e.notes = i.notes();
        e.occurredAt = i.occurredAt();
        return e;
    }

    Interaction toDomain() {
        return new Interaction(id, new CustomerId(customerId), agentId, agentName, channel, direction, subject, notes, occurredAt);
    }
}

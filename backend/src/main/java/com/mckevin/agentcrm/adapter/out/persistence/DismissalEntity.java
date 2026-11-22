package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
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
@Table(name = "recommendation_dismissal")
public class DismissalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationCode code;

    @Column(nullable = false, length = 30)
    private String reference;

    @Column(name = "agent_id", nullable = false)
    private long agentId;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "dismissed_at", nullable = false)
    private Instant dismissedAt;

    protected DismissalEntity() {
    }

    static DismissalEntity from(Dismissal d) {
        DismissalEntity e = new DismissalEntity();
        e.customerId = d.customerId().value();
        e.code = d.code();
        e.reference = d.reference();
        e.agentId = d.agentId();
        e.reason = d.reason();
        e.dismissedAt = d.dismissedAt();
        return e;
    }

    Dismissal toDomain() {
        return new Dismissal(new CustomerId(customerId), code, reference, agentId, reason, dismissedAt);
    }
}

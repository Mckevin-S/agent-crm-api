package com.mckevin.agentcrm.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio_assignment")
public class PortfolioAssignmentEntity {

    @Id
    @Column(name = "customer_id", length = 10)
    private String customerId;

    @Column(name = "agent_id", nullable = false)
    private long agentId;

    protected PortfolioAssignmentEntity() {
    }

    public PortfolioAssignmentEntity(String customerId, long agentId) {
        this.customerId = customerId;
        this.agentId = agentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public long getAgentId() {
        return agentId;
    }
}

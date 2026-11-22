package com.mckevin.agentcrm.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customer_snapshot")
public class SnapshotEntity {

    @Id
    @Column(name = "customer_id", length = 10)
    private String customerId;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "search_text", nullable = false, length = 400)
    private String searchText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected SnapshotEntity() {
    }

    SnapshotEntity(String customerId, String displayName, String searchText, String payload, Instant fetchedAt) {
        this.customerId = customerId;
        this.displayName = displayName;
        this.searchText = searchText;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    String getPayload() {
        return payload;
    }

    Instant getFetchedAt() {
        return fetchedAt;
    }
}

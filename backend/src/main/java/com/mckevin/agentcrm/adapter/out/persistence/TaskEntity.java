package com.mckevin.agentcrm.adapter.out.persistence;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import com.mckevin.agentcrm.domain.task.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "follow_up_task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 10)
    private String customerId;

    @Column(name = "assignee_id", nullable = false)
    private long assigneeId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskStatus status;

    @Column(name = "recommendation_code", length = 30)
    private String recommendationCode;

    @Column(name = "recommendation_ref", length = 30)
    private String recommendationRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected TaskEntity() {
    }

    static TaskEntity from(FollowUpTask t) {
        TaskEntity e = new TaskEntity();
        e.id = t.getId();
        e.customerId = t.getCustomerId().value();
        e.assigneeId = t.getAssigneeId();
        e.title = t.getTitle();
        e.notes = t.getNotes();
        e.priority = t.getPriority();
        e.dueDate = t.getDueDate();
        e.status = t.getStatus();
        e.recommendationCode = t.getRecommendationCode();
        e.recommendationRef = t.getRecommendationRef();
        e.createdAt = t.getCreatedAt();
        e.completedAt = t.getCompletedAt();
        return e;
    }

    FollowUpTask toDomain() {
        return new FollowUpTask(id, new CustomerId(customerId), assigneeId, title, notes, priority, dueDate, status,
                recommendationCode, recommendationRef, createdAt, completedAt);
    }
}

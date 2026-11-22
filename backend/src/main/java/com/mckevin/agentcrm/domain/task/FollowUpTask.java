package com.mckevin.agentcrm.domain.task;

import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.shared.BusinessRuleViolation;
import com.mckevin.agentcrm.domain.shared.Priority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** Tâche de suivi d'un client (relance, rendez-vous, régularisation...), créée à la main ou depuis une recommandation. */
public class FollowUpTask {

    private final Long id;
    private final CustomerId customerId;
    private final long assigneeId;
    private final String title;
    private final String notes;
    private final Priority priority;
    private final String recommendationCode;
    private final String recommendationRef;
    private final Instant createdAt;
    private LocalDate dueDate;
    private TaskStatus status;
    private Instant completedAt;

    @SuppressWarnings("java:S107")
    public FollowUpTask(Long id, CustomerId customerId, long assigneeId, String title, String notes, Priority priority,
                        LocalDate dueDate, TaskStatus status, String recommendationCode, String recommendationRef,
                        Instant createdAt, Instant completedAt) {
        this.id = id;
        this.customerId = Objects.requireNonNull(customerId);
        this.assigneeId = assigneeId;
        this.title = title;
        this.notes = notes;
        this.priority = Objects.requireNonNull(priority);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.status = Objects.requireNonNull(status);
        this.recommendationCode = recommendationCode;
        this.recommendationRef = recommendationRef;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    /** Nouvelle tâche ouverte. L'échéance ne peut pas être dans le passé. */
    @SuppressWarnings("java:S107")
    public static FollowUpTask open(CustomerId customerId, long assigneeId, String title, String notes, Priority priority,
                                    LocalDate dueDate, String recommendationCode, String recommendationRef,
                                    LocalDate today, Instant now) {
        if (title == null || title.isBlank() || title.length() > 160) {
            throw new BusinessRuleViolation("INVALID_TITLE", "Le titre de la tâche est obligatoire (160 caractères au plus).");
        }
        requireNotPast(dueDate, today);
        return new FollowUpTask(null, customerId, assigneeId, title.trim(), notes, priority, dueDate, TaskStatus.OPEN,
                recommendationCode, recommendationRef, now, null);
    }

    public void complete(Instant now) {
        requireOpen();
        status = TaskStatus.DONE;
        completedAt = now;
    }

    public void cancel(Instant now) {
        requireOpen();
        status = TaskStatus.CANCELLED;
        completedAt = now;
    }

    public void reschedule(LocalDate newDueDate, LocalDate today) {
        requireOpen();
        requireNotPast(newDueDate, today);
        dueDate = newDueDate;
    }

    public boolean isOpen() {
        return status == TaskStatus.OPEN;
    }

    public boolean isOverdue(LocalDate today) {
        return isOpen() && dueDate.isBefore(today);
    }

    public boolean comesFrom(String code, String reference) {
        return Objects.equals(recommendationCode, code) && Objects.equals(recommendationRef, reference);
    }

    private void requireOpen() {
        if (status != TaskStatus.OPEN) {
            throw new BusinessRuleViolation("TASK_NOT_OPEN", "Cette tâche est déjà terminée ou annulée.");
        }
    }

    private static void requireNotPast(LocalDate dueDate, LocalDate today) {
        if (dueDate == null || dueDate.isBefore(today)) {
            throw new BusinessRuleViolation("DUE_DATE_IN_PAST", "L'échéance ne peut pas être dans le passé.");
        }
    }

    public Long getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public long getAssigneeId() {
        return assigneeId;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getRecommendationCode() {
        return recommendationCode;
    }

    public String getRecommendationRef() {
        return recommendationRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.Views.TaskItem;
import com.mckevin.agentcrm.application.port.DismissalRepository;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.PortfolioRepository;
import com.mckevin.agentcrm.application.port.ProfileSnapshots;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.interaction.Channel;
import com.mckevin.agentcrm.domain.interaction.Direction;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
import com.mckevin.agentcrm.domain.shared.BusinessRuleViolation;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Suivi des clients : échanges, tâches et traitement des recommandations. */
public class FollowUpService {

    public record LogInteractionCommand(Channel channel, Direction direction, String subject, String notes,
                                        Instant occurredAt, LocalDate followUpOn, String followUpTitle) {
    }

    public record CreateTaskCommand(String title, String notes, Priority priority, LocalDate dueDate, Long assigneeId) {
    }

    private final CustomerService customers;
    private final ProfileLoader loader;
    private final RecommendationFinder finder;
    private final InteractionRepository interactions;
    private final TaskRepository tasks;
    private final DismissalRepository dismissals;
    private final PortfolioRepository portfolios;
    private final ProfileSnapshots snapshots;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public FollowUpService(CustomerService customers, ProfileLoader loader, RecommendationFinder finder,
                           InteractionRepository interactions, TaskRepository tasks, DismissalRepository dismissals,
                           PortfolioRepository portfolios, ProfileSnapshots snapshots, Clock clock) {
        this.customers = customers;
        this.loader = loader;
        this.finder = finder;
        this.interactions = interactions;
        this.tasks = tasks;
        this.dismissals = dismissals;
        this.portfolios = portfolios;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    /** Trace un échange ; si une date de relance est donnée, crée aussi la tâche correspondante. */
    @Transactional
    public Interaction logInteraction(Agent agent, CustomerId customerId, LogInteractionCommand command) {
        customers.requireAccess(agent, customerId);
        Instant now = Instant.now(clock);
        Instant occurredAt = command.occurredAt() == null ? now : command.occurredAt();
        Interaction saved = interactions.save(Interaction.log(customerId, agent.id(), agent.fullName(), command.channel(),
                command.direction(), command.subject(), command.notes(), occurredAt, now));
        if (command.followUpOn() != null) {
            String title = command.followUpTitle() == null || command.followUpTitle().isBlank()
                    ? "Relance : " + saved.subject() : command.followUpTitle();
            tasks.save(FollowUpTask.open(customerId, assigneeFor(customerId, agent), title, null, Priority.MEDIUM,
                    command.followUpOn(), null, null, today(), now));
        }
        return saved;
    }

    @Transactional
    public FollowUpTask createTask(Agent agent, CustomerId customerId, CreateTaskCommand command) {
        customers.requireAccess(agent, customerId);
        long assignee = command.assigneeId() != null && agent.manager() ? command.assigneeId() : assigneeFor(customerId, agent);
        return tasks.save(FollowUpTask.open(customerId, assignee, command.title(), command.notes(),
                command.priority() == null ? Priority.MEDIUM : command.priority(), command.dueDate(), null, null,
                today(), Instant.now(clock)));
    }

    @Transactional
    public FollowUpTask completeTask(Agent agent, long taskId) {
        FollowUpTask task = ownTask(agent, taskId);
        task.complete(Instant.now(clock));
        return tasks.save(task);
    }

    @Transactional
    public FollowUpTask rescheduleTask(Agent agent, long taskId, LocalDate dueDate) {
        FollowUpTask task = ownTask(agent, taskId);
        task.reschedule(dueDate, today());
        return tasks.save(task);
    }

    /** Accepter une recommandation la transforme en tâche (elle disparaît alors de la liste). */
    @Transactional
    public FollowUpTask accept(Agent agent, CustomerId customerId, RecommendationCode code, String reference,
                               LocalDate dueDate) {
        customers.requireAccess(agent, customerId);
        Recommendation recommendation = currentRecommendation(customerId, code, reference);
        LocalDate due = dueDate != null ? dueDate : recommendation.suggestedDueDate();
        return tasks.save(FollowUpTask.open(customerId, assigneeFor(customerId, agent), recommendation.title(),
                recommendation.rationale(), recommendation.priority(), due, code.name(), recommendation.reference(),
                today(), Instant.now(clock)));
    }

    /** Écarter une recommandation la masque pendant 90 jours ; le motif est conservé. */
    @Transactional
    public void dismiss(Agent agent, CustomerId customerId, RecommendationCode code, String reference, String reason) {
        customers.requireAccess(agent, customerId);
        Recommendation recommendation = currentRecommendation(customerId, code, reference);
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleViolation("REASON_REQUIRED", "Indiquez pourquoi la recommandation est écartée.");
        }
        dismissals.save(new Dismissal(customerId, code, recommendation.reference(), agent.id(), reason.trim(),
                Instant.now(clock)));
    }

    /** Tâches d'un conseiller ; un manager peut consulter celles d'un autre conseiller. */
    @Transactional(readOnly = true)
    public List<TaskItem> tasksOf(Agent agent, Long agentId, boolean openOnly) {
        long target = agentId != null && agent.manager() ? agentId : agent.id();
        List<FollowUpTask> list = openOnly ? tasks.findOpenByAssignee(target) : tasks.findByAssignee(target);
        return list.stream()
                .sorted(Comparator.comparing(FollowUpTask::isOpen).reversed().thenComparing(FollowUpTask::getDueDate))
                .map(t -> new TaskItem(t, customerName(t.getCustomerId())))
                .toList();
    }

    private Recommendation currentRecommendation(CustomerId customerId, RecommendationCode code, String reference) {
        return finder.recommendationsFor(loader.load(customerId).profile()).stream()
                .filter(r -> r.matches(code, reference))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolation("RECOMMENDATION_NOT_AVAILABLE",
                        "Cette recommandation n'est plus d'actualité pour ce client."));
    }

    private FollowUpTask ownTask(Agent agent, long taskId) {
        FollowUpTask task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Tâche introuvable."));
        if (!agent.manager() && task.getAssigneeId() != agent.id()) {
            throw new AccessDeniedException("Cette tâche est assignée à un autre conseiller.");
        }
        return task;
    }

    private long assigneeFor(CustomerId customerId, Agent agent) {
        return portfolios.advisorOf(customerId).orElse(agent.id());
    }

    private String customerName(CustomerId id) {
        return snapshots.find(id).map(s -> s.profile().customer().fullName()).orElse(id.value());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}

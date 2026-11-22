package com.mckevin.agentcrm.support;

import com.mckevin.agentcrm.application.port.AgentDirectory;
import com.mckevin.agentcrm.application.port.CrmGateway;
import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.application.port.DismissalRepository;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.PortfolioRepository;
import com.mckevin.agentcrm.application.port.ProfileSnapshots;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;
import com.mckevin.agentcrm.domain.task.FollowUpTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implémentations en mémoire des ports : elles permettent de tester les cas d'usage et les scénarios BDD
 * sans base de données ni réseau. C'est le bénéfice direct de l'architecture hexagonale.
 */
public final class InMemoryAdapters {

    private InMemoryAdapters() {
    }

    public static class Crm implements CrmGateway {
        private final Map<CustomerId, CustomerProfile> profiles = new LinkedHashMap<>();
        private boolean down;

        public void put(CustomerProfile profile) {
            profiles.put(profile.customer().id(), profile);
        }

        public void setDown(boolean down) {
            this.down = down;
        }

        @Override
        public List<Customer> search(String query) {
            failIfDown();
            String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
            return profiles.values().stream().map(CustomerProfile::customer)
                    .filter(c -> c.fullName().toLowerCase(Locale.ROOT).contains(q)).toList();
        }

        @Override
        public Optional<CustomerProfile> fetchProfile(CustomerId id) {
            failIfDown();
            return Optional.ofNullable(profiles.get(id));
        }

        private void failIfDown() {
            if (down) {
                throw new CrmUnavailableException("CRM indisponible (test)", null);
            }
        }
    }

    public static class Snapshots implements ProfileSnapshots {
        private final Map<CustomerId, Snapshot> store = new HashMap<>();

        @Override
        public void save(CustomerProfile profile, Instant fetchedAt) {
            store.put(profile.customer().id(), new Snapshot(profile, fetchedAt));
        }

        @Override
        public Optional<Snapshot> find(CustomerId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Customer> search(String query) {
            String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
            return store.values().stream().map(s -> s.profile().customer())
                    .filter(c -> c.fullName().toLowerCase(Locale.ROOT).contains(q)).toList();
        }
    }

    public static class Interactions implements InteractionRepository {
        private final List<Interaction> items = new ArrayList<>();
        private final AtomicLong ids = new AtomicLong();

        @Override
        public Interaction save(Interaction i) {
            Interaction saved = new Interaction(ids.incrementAndGet(), i.customerId(), i.agentId(), i.agentName(), i.channel(),
                    i.direction(), i.subject(), i.notes(), i.occurredAt());
            items.add(saved);
            return saved;
        }

        @Override
        public List<Interaction> findByCustomer(CustomerId customerId) {
            return items.stream().filter(i -> i.customerId().equals(customerId)).toList();
        }

        @Override
        public Optional<Instant> lastInteraction(CustomerId customerId) {
            return findByCustomer(customerId).stream().map(Interaction::occurredAt).max(Comparator.naturalOrder());
        }

        @Override
        public Map<CustomerId, Instant> lastInteractions(Collection<CustomerId> customerIds) {
            Map<CustomerId, Instant> result = new HashMap<>();
            customerIds.forEach(id -> lastInteraction(id).ifPresent(t -> result.put(id, t)));
            return result;
        }

        @Override
        public long countByAgentSince(long agentId, Instant since) {
            return items.stream().filter(i -> i.agentId() == agentId && i.occurredAt().isAfter(since)).count();
        }
    }

    public static class Tasks implements TaskRepository {
        private final Map<Long, FollowUpTask> items = new LinkedHashMap<>();
        private final AtomicLong ids = new AtomicLong();

        @Override
        public FollowUpTask save(FollowUpTask t) {
            long id = t.getId() != null ? t.getId() : ids.incrementAndGet();
            FollowUpTask saved = new FollowUpTask(id, t.getCustomerId(), t.getAssigneeId(), t.getTitle(), t.getNotes(),
                    t.getPriority(), t.getDueDate(), t.getStatus(), t.getRecommendationCode(), t.getRecommendationRef(),
                    t.getCreatedAt(), t.getCompletedAt());
            items.put(id, saved);
            return saved;
        }

        @Override
        public Optional<FollowUpTask> findById(long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public List<FollowUpTask> findByCustomer(CustomerId customerId) {
            return items.values().stream().filter(t -> t.getCustomerId().equals(customerId)).toList();
        }

        @Override
        public List<FollowUpTask> findOpenByAssignee(long assigneeId) {
            return items.values().stream().filter(t -> t.getAssigneeId() == assigneeId && t.isOpen()).toList();
        }

        @Override
        public List<FollowUpTask> findAllOpen() {
            return items.values().stream().filter(FollowUpTask::isOpen).toList();
        }

        @Override
        public List<FollowUpTask> findByAssignee(long assigneeId) {
            return items.values().stream().filter(t -> t.getAssigneeId() == assigneeId).toList();
        }
    }

    public static class Dismissals implements DismissalRepository {
        private final List<Dismissal> items = new ArrayList<>();

        @Override
        public void save(Dismissal dismissal) {
            items.add(dismissal);
        }

        @Override
        public List<Dismissal> findByCustomer(CustomerId customerId) {
            return items.stream().filter(d -> d.customerId().equals(customerId)).toList();
        }
    }

    public static class Portfolio implements PortfolioRepository {
        private final Map<CustomerId, Long> assignments = new LinkedHashMap<>();

        public void assign(CustomerId customerId, long agentId) {
            assignments.put(customerId, agentId);
        }

        @Override
        public List<CustomerId> customersOf(long agentId) {
            return assignments.entrySet().stream().filter(e -> e.getValue() == agentId).map(Map.Entry::getKey).toList();
        }

        @Override
        public List<CustomerId> allCustomers() {
            return List.copyOf(assignments.keySet());
        }

        @Override
        public Optional<Long> advisorOf(CustomerId customerId) {
            return Optional.ofNullable(assignments.get(customerId));
        }
    }

    public static class Agents implements AgentDirectory {
        private final Map<Long, AgentSummary> agents = new LinkedHashMap<>();

        public void add(AgentSummary agent) {
            agents.put(agent.id(), agent);
        }

        @Override
        public List<AgentSummary> agents() {
            return List.copyOf(agents.values());
        }

        @Override
        public Optional<AgentSummary> find(long id) {
            return Optional.ofNullable(agents.get(id));
        }
    }
}

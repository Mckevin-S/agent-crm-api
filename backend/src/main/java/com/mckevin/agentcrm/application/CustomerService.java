package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.ProfileLoader.LoadedProfile;
import com.mckevin.agentcrm.application.Views.Customer360;
import com.mckevin.agentcrm.application.Views.CustomerSummary;
import com.mckevin.agentcrm.application.Views.Kpis;
import com.mckevin.agentcrm.application.Views.SearchResult;
import com.mckevin.agentcrm.application.port.AgentDirectory;
import com.mckevin.agentcrm.application.port.CrmGateway;
import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.PortfolioRepository;
import com.mckevin.agentcrm.application.port.ProfileSnapshots;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Recherche de clients et vue client à 360°. */
public class CustomerService {

    private final CrmGateway crm;
    private final ProfileSnapshots snapshots;
    private final ProfileLoader loader;
    private final PortfolioRepository portfolios;
    private final AgentDirectory agents;
    private final InteractionRepository interactions;
    private final TaskRepository tasks;
    private final RecommendationFinder finder;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public CustomerService(CrmGateway crm, ProfileSnapshots snapshots, ProfileLoader loader, PortfolioRepository portfolios,
                           AgentDirectory agents, InteractionRepository interactions, TaskRepository tasks,
                           RecommendationFinder finder, Clock clock) {
        this.crm = crm;
        this.snapshots = snapshots;
        this.loader = loader;
        this.portfolios = portfolios;
        this.agents = agents;
        this.interactions = interactions;
        this.tasks = tasks;
        this.finder = finder;
        this.clock = clock;
    }

    /** Recherche dans le CRM ; en mode dégradé, dans les copies locales. Un conseiller ne voit que son portefeuille. */
    @Transactional(readOnly = true)
    public SearchResult search(Agent agent, String query) {
        List<Customer> found;
        boolean live = true;
        try {
            found = crm.search(query);
        } catch (CrmUnavailableException e) {
            found = snapshots.search(query);
            live = false;
        }
        Set<CustomerId> mine = agent.manager() ? Set.of() : new HashSet<>(portfolios.customersOf(agent.id()));
        List<CustomerSummary> summaries = found.stream()
                .filter(c -> agent.manager() || mine.contains(c.id()))
                .map(c -> new CustomerSummary(c, advisorName(c.id())))
                .toList();
        return new SearchResult(summaries, live);
    }

    @Transactional
    public Customer360 view(Agent agent, CustomerId id) {
        requireAccess(agent, id);
        LoadedProfile loaded = loader.load(id);
        CustomerProfile profile = loaded.profile();
        LocalDate today = today();
        List<FollowUpTask> customerTasks = tasks.findByCustomer(id);
        List<Interaction> history = interactions.findByCustomer(id).stream()
                .sorted(Comparator.comparing(Interaction::occurredAt).reversed())
                .toList();
        Optional<Instant> last = history.stream().map(Interaction::occurredAt).findFirst();
        List<Recommendation> recommendations = finder.recommendationsFor(profile, last);
        Kpis kpis = new Kpis(profile.annualPremium(), profile.activeContracts().size(), profile.pendingClaims().size(),
                profile.customer().seniorityYearsOn(today), profile.customer().ageOn(today));
        List<FollowUpTask> open = customerTasks.stream().filter(FollowUpTask::isOpen)
                .sorted(Comparator.comparing(FollowUpTask::getDueDate)).toList();
        List<FollowUpTask> closed = customerTasks.stream().filter(t -> !t.isOpen())
                .sorted(Comparator.comparing(FollowUpTask::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return new Customer360(profile, loaded.live(), loaded.fetchedAt(), advisorName(id), kpis, recommendations,
                open, closed, history);
    }

    /** Un manager voit toute l'agence ; un conseiller, uniquement les clients de son portefeuille. */
    void requireAccess(Agent agent, CustomerId id) {
        if (agent.manager()) {
            return;
        }
        if (portfolios.advisorOf(id).filter(advisor -> advisor == agent.id()).isEmpty()) {
            throw new AccessDeniedException("Ce client ne fait pas partie de votre portefeuille.");
        }
    }

    private String advisorName(CustomerId id) {
        return portfolios.advisorOf(id).flatMap(agents::find).map(AgentDirectory.AgentSummary::fullName).orElse(null);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}

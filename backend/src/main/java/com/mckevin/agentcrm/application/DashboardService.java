package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.application.ProfileLoader.LoadedProfile;
import com.mckevin.agentcrm.application.Views.Dashboard;
import com.mckevin.agentcrm.application.Views.PortfolioRecommendation;
import com.mckevin.agentcrm.application.Views.TaskItem;
import com.mckevin.agentcrm.application.Views.TeamMember;
import com.mckevin.agentcrm.application.port.AgentDirectory;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.PortfolioRepository;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.domain.customer.Contract;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Tableau de bord du conseiller et vue d'équipe du manager. */
public class DashboardService {

    private static final int MAX_RECOMMENDATIONS = 8;
    private static final int UPCOMING_DAYS = 7;

    private final ProfileLoader loader;
    private final RecommendationFinder finder;
    private final PortfolioRepository portfolios;
    private final TaskRepository tasks;
    private final InteractionRepository interactions;
    private final AgentDirectory agents;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public DashboardService(ProfileLoader loader, RecommendationFinder finder, PortfolioRepository portfolios,
                            TaskRepository tasks, InteractionRepository interactions, AgentDirectory agents, Clock clock) {
        this.loader = loader;
        this.finder = finder;
        this.portfolios = portfolios;
        this.tasks = tasks;
        this.interactions = interactions;
        this.agents = agents;
        this.clock = clock;
    }

    @Transactional
    public Dashboard dashboard(Agent agent) {
        LocalDate today = LocalDate.now(clock);
        // Un manager pilote l'agence entière ; un conseiller, son portefeuille
        List<CustomerId> portfolio = agent.manager() ? portfolios.allCustomers() : portfolios.customersOf(agent.id());
        Map<CustomerId, Instant> lastContacts = interactions.lastInteractions(portfolio);
        Map<CustomerId, String> names = new HashMap<>();
        List<PortfolioRecommendation> recommendations = new ArrayList<>();
        BigDecimal premium = BigDecimal.ZERO;
        int renewals = 0;
        int claims = 0;
        boolean live = true;

        for (CustomerId id : portfolio) {
            var loaded = loader.tryLoad(id);
            if (loaded.isEmpty()) {
                live = false;
                continue;
            }
            LoadedProfile lp = loaded.get();
            live &= lp.live();
            var profile = lp.profile();
            names.put(id, profile.customer().fullName());
            premium = premium.add(profile.annualPremium());
            renewals += (int) profile.activeContracts().stream().map(Contract::renewalDate)
                    .filter(d -> !d.isBefore(today) && !d.isAfter(today.plusDays(30))).count();
            claims += profile.pendingClaims().size();
            for (Recommendation r : finder.recommendationsFor(profile, Optional.ofNullable(lastContacts.get(id)))) {
                recommendations.add(new PortfolioRecommendation(id.value(), profile.customer().fullName(), r));
            }
        }

        List<FollowUpTask> open = agent.manager() ? tasks.findAllOpen() : tasks.findOpenByAssignee(agent.id());
        int overdue = (int) open.stream().filter(t -> t.isOverdue(today)).count();
        int dueToday = (int) open.stream().filter(t -> t.getDueDate().isEqual(today)).count();
        List<TaskItem> upcoming = open.stream()
                .filter(t -> !t.getDueDate().isAfter(today.plusDays(UPCOMING_DAYS)))
                .sorted(Comparator.comparing(FollowUpTask::getDueDate))
                .map(t -> new TaskItem(t, names.getOrDefault(t.getCustomerId(), t.getCustomerId().value())))
                .toList();
        List<PortfolioRecommendation> top = recommendations.stream()
                .sorted(Comparator.comparing((PortfolioRecommendation p) -> p.recommendation().priority()).reversed()
                        .thenComparing(p -> p.recommendation().suggestedDueDate()))
                .limit(MAX_RECOMMENDATIONS)
                .toList();
        return new Dashboard(portfolio.size(), premium, renewals, claims, open.size(), overdue, dueToday, top, upcoming, live);
    }

    /** Vue manager : charge et activité de chaque conseiller. */
    @Transactional
    public List<TeamMember> team(Agent manager) {
        if (!manager.manager()) {
            throw new AccessDeniedException("La vue équipe est réservée aux managers.");
        }
        LocalDate today = LocalDate.now(clock);
        Instant since = Instant.now(clock).minus(Duration.ofDays(30));
        return agents.agents().stream()
                .filter(a -> !a.manager())
                .map(a -> {
                    List<CustomerId> portfolio = portfolios.customersOf(a.id());
                    Map<CustomerId, Instant> last = interactions.lastInteractions(portfolio);
                    int recs = portfolio.stream()
                            .map(loader::tryLoad)
                            .flatMap(Optional::stream)
                            .mapToInt(lp -> finder.recommendationsFor(lp.profile(),
                                    Optional.ofNullable(last.get(lp.profile().customer().id()))).size())
                            .sum();
                    List<FollowUpTask> open = tasks.findOpenByAssignee(a.id());
                    return new TeamMember(a.id(), a.fullName(), portfolio.size(), open.size(),
                            (int) open.stream().filter(t -> t.isOverdue(today)).count(), recs,
                            interactions.countByAgentSince(a.id(), since));
                })
                .toList();
    }
}

package com.mckevin.agentcrm.application;

import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.task.FollowUpTask;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Résultats des cas d'usage, indépendants du format d'exposition (REST). */
public final class Views {

    private Views() {
    }

    public record CustomerSummary(Customer customer, String advisorName) {
    }

    public record SearchResult(List<CustomerSummary> customers, boolean live) {
    }

    public record Kpis(BigDecimal annualPremium, int activeContracts, int pendingClaims, int seniorityYears, int age) {
    }

    public record Customer360(CustomerProfile profile, boolean live, Instant fetchedAt, String advisorName, Kpis kpis,
                              List<Recommendation> recommendations, List<FollowUpTask> openTasks,
                              List<FollowUpTask> closedTasks, List<Interaction> interactions) {
    }

    public record TaskItem(FollowUpTask task, String customerName) {
    }

    public record PortfolioRecommendation(String customerId, String customerName, Recommendation recommendation) {
    }

    public record Dashboard(int customers, BigDecimal annualPremium, int renewalsNext30Days, int pendingClaims,
                            int openTasks, int overdueTasks, int dueToday, List<PortfolioRecommendation> recommendations,
                            List<TaskItem> upcomingTasks, boolean live) {
    }

    public record TeamMember(long agentId, String fullName, int customers, int openTasks, int overdueTasks,
                             int openRecommendations, long interactionsLast30Days) {
    }
}

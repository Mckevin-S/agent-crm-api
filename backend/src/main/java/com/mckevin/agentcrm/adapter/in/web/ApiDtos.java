package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.application.CrmService;
import com.mckevin.agentcrm.application.Views;
import com.mckevin.agentcrm.domain.customer.Claim;
import com.mckevin.agentcrm.domain.customer.Contract;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.interaction.Channel;
import com.mckevin.agentcrm.domain.interaction.Direction;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import com.mckevin.agentcrm.domain.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contrat d'échange JSON de l'API. Les libellés français sont fournis pour simplifier l'affichage. */
public final class ApiDtos {

    private ApiDtos() {
    }

    // ----- Requêtes -----

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record InteractionRequest(@NotNull Channel channel, @NotNull Direction direction,
                                     @NotBlank @Size(max = 120) String subject, @Size(max = 2000) String notes,
                                     Instant occurredAt, LocalDate followUpOn, @Size(max = 160) String followUpTitle) {
    }

    public record TaskRequest(@NotBlank @Size(max = 160) String title, @Size(max = 2000) String notes, Priority priority,
                              @NotNull LocalDate dueDate, Long assigneeId) {
    }

    public record AcceptRequest(String reference, LocalDate dueDate) {
    }

    public record DismissRequest(String reference, @NotBlank @Size(max = 300) String reason) {
    }

    public record RescheduleRequest(@NotNull LocalDate dueDate) {
    }

    public record OutageRequest(boolean outage) {
    }

    // ----- Réponses -----

    public record AgentDto(long id, String fullName, String email, boolean manager, String initials) {
    }

    public record SessionDto(String token, Instant expiresAt, AgentDto agent) {
    }

    public record CustomerDto(String id, String title, String firstName, String lastName, String fullName,
                              LocalDate birthDate, String email, String phone, String city, String postalCode,
                              String segment, String segmentLabel, LocalDate customerSince, String initials) {

        static CustomerDto of(Customer c) {
            return new CustomerDto(c.id().value(), c.title(), c.firstName(), c.lastName(), c.fullName(), c.birthDate(),
                    c.email(), c.phone(), c.city(), c.postalCode(), c.segment().name(), c.segment().label(),
                    c.customerSince(), initials(c.fullName()));
        }
    }

    public record CustomerSummaryDto(CustomerDto customer, String advisorName) {
    }

    public record SearchDto(List<CustomerSummaryDto> customers, boolean live) {

        static SearchDto of(Views.SearchResult r) {
            return new SearchDto(r.customers().stream()
                    .map(s -> new CustomerSummaryDto(CustomerDto.of(s.customer()), s.advisorName())).toList(), r.live());
        }
    }

    public record ContractDto(String number, String product, String productLabel, String label, String status,
                              String statusLabel, BigDecimal annualPremium, LocalDate startDate, LocalDate renewalDate,
                              long daysUntilRenewal) {

        static ContractDto of(Contract c, LocalDate today) {
            return new ContractDto(c.number(), c.product().name(), c.product().label(), c.label(), c.status().name(),
                    c.status().label(), c.annualPremium(), c.startDate(), c.renewalDate(), c.daysUntilRenewal(today));
        }
    }

    public record ClaimDto(String number, String contractNumber, String nature, String status, String statusLabel,
                           boolean pending, LocalDate openedOn, long ageInDays, BigDecimal estimatedAmount) {

        static ClaimDto of(Claim c, LocalDate today) {
            return new ClaimDto(c.number(), c.contractNumber(), c.nature(), c.status().name(), c.status().label(),
                    c.status().isPending(), c.openedOn(), c.ageInDays(today), c.estimatedAmount());
        }
    }

    public record RecommendationDto(String code, String reference, String priority, String priorityLabel, String title,
                                    String rationale, LocalDate suggestedDueDate) {

        static RecommendationDto of(Recommendation r) {
            return new RecommendationDto(r.code().name(), r.reference(), r.priority().name(), r.priority().label(),
                    r.title(), r.rationale(), r.suggestedDueDate());
        }
    }

    public record TaskDto(long id, String customerId, String customerName, String title, String notes, String priority,
                          String priorityLabel, LocalDate dueDate, TaskStatus status, boolean overdue,
                          String recommendationCode, Instant createdAt, Instant completedAt) {

        static TaskDto of(FollowUpTask t, String customerName, LocalDate today) {
            return new TaskDto(t.getId(), t.getCustomerId().value(), customerName, t.getTitle(), t.getNotes(),
                    t.getPriority().name(), t.getPriority().label(), t.getDueDate(), t.getStatus(), t.isOverdue(today),
                    t.getRecommendationCode(), t.getCreatedAt(), t.getCompletedAt());
        }
    }

    public record InteractionDto(long id, String channel, String channelLabel, String direction, String subject,
                                 String notes, String agentName, Instant occurredAt) {

        static InteractionDto of(Interaction i) {
            return new InteractionDto(i.id(), i.channel().name(), i.channel().label(), i.direction().name(), i.subject(),
                    i.notes(), i.agentName(), i.occurredAt());
        }
    }

    public record KpisDto(BigDecimal annualPremium, int activeContracts, int pendingClaims, int seniorityYears, int age) {
    }

    public record Customer360Dto(CustomerDto customer, String advisorName, boolean live, Instant fetchedAt, KpisDto kpis,
                                 List<RecommendationDto> recommendations, List<ContractDto> contracts,
                                 List<ClaimDto> claims, List<TaskDto> openTasks, List<TaskDto> closedTasks,
                                 List<InteractionDto> interactions) {

        static Customer360Dto of(Views.Customer360 v, LocalDate today) {
            String name = v.profile().customer().fullName();
            Views.Kpis k = v.kpis();
            return new Customer360Dto(CustomerDto.of(v.profile().customer()), v.advisorName(), v.live(), v.fetchedAt(),
                    new KpisDto(k.annualPremium(), k.activeContracts(), k.pendingClaims(), k.seniorityYears(), k.age()),
                    v.recommendations().stream().map(RecommendationDto::of).toList(),
                    v.profile().contracts().stream().map(c -> ContractDto.of(c, today)).toList(),
                    v.profile().claims().stream().map(c -> ClaimDto.of(c, today)).toList(),
                    v.openTasks().stream().map(t -> TaskDto.of(t, name, today)).toList(),
                    v.closedTasks().stream().map(t -> TaskDto.of(t, name, today)).toList(),
                    v.interactions().stream().map(InteractionDto::of).toList());
        }
    }

    public record PortfolioRecommendationDto(String customerId, String customerName, RecommendationDto recommendation) {
    }

    public record DashboardDto(int customers, BigDecimal annualPremium, int renewalsNext30Days, int pendingClaims,
                               int openTasks, int overdueTasks, int dueToday,
                               List<PortfolioRecommendationDto> recommendations, List<TaskDto> upcomingTasks, boolean live) {

        static DashboardDto of(Views.Dashboard d, LocalDate today) {
            return new DashboardDto(d.customers(), d.annualPremium(), d.renewalsNext30Days(), d.pendingClaims(),
                    d.openTasks(), d.overdueTasks(), d.dueToday(),
                    d.recommendations().stream().map(r -> new PortfolioRecommendationDto(r.customerId(), r.customerName(),
                            RecommendationDto.of(r.recommendation()))).toList(),
                    d.upcomingTasks().stream().map(t -> TaskDto.of(t.task(), t.customerName(), today)).toList(),
                    d.live());
        }
    }

    public record CrmStateDto(boolean available, String circuitState, Instant lastSuccessAt, Instant lastFailureAt,
                              boolean outageSimulated) {

        static CrmStateDto of(CrmService.CrmState s) {
            return new CrmStateDto(s.status().available(), s.status().circuitState(), s.status().lastSuccessAt(),
                    s.status().lastFailureAt(), s.outageSimulated());
        }
    }

    static String initials(String fullName) {
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }
}

package com.mckevin.agentcrm.config;

import com.mckevin.agentcrm.application.AuthenticationService;
import com.mckevin.agentcrm.application.CrmService;
import com.mckevin.agentcrm.application.CustomerService;
import com.mckevin.agentcrm.application.DashboardService;
import com.mckevin.agentcrm.application.FollowUpService;
import com.mckevin.agentcrm.application.ProfileLoader;
import com.mckevin.agentcrm.application.RecommendationFinder;
import com.mckevin.agentcrm.application.port.AgentAccounts;
import com.mckevin.agentcrm.application.port.AgentDirectory;
import com.mckevin.agentcrm.application.port.CrmFaultInjector;
import com.mckevin.agentcrm.application.port.CrmGateway;
import com.mckevin.agentcrm.application.port.CrmStatusProvider;
import com.mckevin.agentcrm.application.port.DismissalRepository;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.PasswordVerifier;
import com.mckevin.agentcrm.application.port.PortfolioRepository;
import com.mckevin.agentcrm.application.port.ProfileSnapshots;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.application.port.TokenIssuer;
import com.mckevin.agentcrm.domain.recommendation.NextBestActionEngine;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Assemblage de l'hexagone : le domaine et les cas d'usage ne connaissent pas Spring,
 * c'est ici qu'ils sont instanciés et reliés aux adaptateurs.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }

    @Bean
    public NextBestActionEngine nextBestActionEngine() {
        return NextBestActionEngine.withDefaultRules();
    }

    @Bean
    public ProfileLoader profileLoader(CrmGateway crm, ProfileSnapshots snapshots, Clock clock) {
        return new ProfileLoader(crm, snapshots, clock);
    }

    @Bean
    public RecommendationFinder recommendationFinder(InteractionRepository interactions, TaskRepository tasks,
                                                     DismissalRepository dismissals, NextBestActionEngine engine,
                                                     Clock clock) {
        return new RecommendationFinder(interactions, tasks, dismissals, engine, clock);
    }

    @Bean
    @SuppressWarnings("java:S107")
    public CustomerService customerService(CrmGateway crm, ProfileSnapshots snapshots, ProfileLoader loader,
                                           PortfolioRepository portfolios, AgentDirectory agents,
                                           InteractionRepository interactions, TaskRepository tasks,
                                           RecommendationFinder finder, Clock clock) {
        return new CustomerService(crm, snapshots, loader, portfolios, agents, interactions, tasks, finder, clock);
    }

    @Bean
    @SuppressWarnings("java:S107")
    public FollowUpService followUpService(CustomerService customers, ProfileLoader loader, RecommendationFinder finder,
                                           InteractionRepository interactions, TaskRepository tasks,
                                           DismissalRepository dismissals, PortfolioRepository portfolios,
                                           ProfileSnapshots snapshots, Clock clock) {
        return new FollowUpService(customers, loader, finder, interactions, tasks, dismissals, portfolios, snapshots, clock);
    }

    @Bean
    public DashboardService dashboardService(ProfileLoader loader, RecommendationFinder finder,
                                             PortfolioRepository portfolios, TaskRepository tasks,
                                             InteractionRepository interactions, AgentDirectory agents, Clock clock) {
        return new DashboardService(loader, finder, portfolios, tasks, interactions, agents, clock);
    }

    @Bean
    public CrmService crmService(CrmStatusProvider status, CrmFaultInjector faults) {
        return new CrmService(status, faults);
    }

    @Bean
    public AuthenticationService authenticationService(AgentAccounts accounts, PasswordVerifier passwords,
                                                       TokenIssuer tokens) {
        return new AuthenticationService(accounts, passwords, tokens);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Agent CRM API").version("1.0.0")
                        .description("Poste de travail du conseiller : vue client 360, recommandations, suivi."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

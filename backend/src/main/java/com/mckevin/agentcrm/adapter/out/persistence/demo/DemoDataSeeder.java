package com.mckevin.agentcrm.adapter.out.persistence.demo;

import com.mckevin.agentcrm.adapter.out.persistence.UserEntity;
import com.mckevin.agentcrm.adapter.out.persistence.UserSpringRepository;
import com.mckevin.agentcrm.application.port.DismissalRepository;
import com.mckevin.agentcrm.application.port.InteractionRepository;
import com.mckevin.agentcrm.application.port.TaskRepository;
import com.mckevin.agentcrm.adapter.out.persistence.PortfolioAssignmentEntity;
import com.mckevin.agentcrm.adapter.out.persistence.PortfolioSpringRepository;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.interaction.Channel;
import com.mckevin.agentcrm.domain.interaction.Direction;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import com.mckevin.agentcrm.domain.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Données de démonstration : une agence de trois personnes (un manager, deux conseillers), la répartition
 * des 14 clients du CRM simulé, un historique d'échanges et des tâches. Activé par DEMO_DATA=true sur une base vide.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "demo1234";
    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserSpringRepository users;
    private final PortfolioSpringRepository portfolio;
    private final InteractionRepository interactions;
    private final TaskRepository tasks;
    private final DismissalRepository dismissals;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public DemoDataSeeder(UserSpringRepository users, PortfolioSpringRepository portfolio, InteractionRepository interactions,
                          TaskRepository tasks, DismissalRepository dismissals, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.portfolio = portfolio;
        this.interactions = interactions;
        this.tasks = tasks;
        this.dismissals = dismissals;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            LOG.info("Données de démonstration ignorées : la base contient déjà des comptes.");
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        users.save(new UserEntity("sophie@demo.fr", "Sophie Bernard", hash, UserEntity.Role.MANAGER));
        UserEntity mckevin = users.save(new UserEntity("mckevin@demo.fr", "Mckevin Kamguem", hash, UserEntity.Role.AGENT));
        UserEntity lucas = users.save(new UserEntity("lucas@demo.fr", "Lucas Morel", hash, UserEntity.Role.AGENT));

        List.of("C-10234", "C-10235", "C-10236", "C-10237", "C-10238", "C-10239", "C-10240", "C-10241")
                .forEach(id -> portfolio.save(new PortfolioAssignmentEntity(id, mckevin.getId())));
        List.of("C-10242", "C-10243", "C-10244", "C-10245", "C-10246", "C-10247")
                .forEach(id -> portfolio.save(new PortfolioAssignmentEntity(id, lucas.getId())));

        Agent m = new Agent(mckevin.getId(), mckevin.getFullName());
        Agent l = new Agent(lucas.getId(), lucas.getFullName());
        interaction(m, "C-10234", Channel.PHONE, Direction.INBOUND, "Question sur la franchise bris de glace",
                "Explication du barème ; cliente satisfaite.", 20);
        interaction(m, "C-10235", Channel.MEETING, Direction.OUTBOUND, "Déclaration du dégât des eaux",
                "Constat amiable rempli avec le voisin du dessus. Expert mandaté.", 40);
        interaction(m, "C-10237", Channel.EMAIL, Direction.OUTBOUND, "Relance cotisation habitation impayée",
                "Deuxième relance envoyée ; risque de suspension.", 90);
        interaction(m, "C-10238", Channel.VIDEO, Direction.OUTBOUND, "Bilan patrimonial annuel",
                "Point épargne retraite et prévoyance, aucun changement souhaité.", 15);
        interaction(m, "C-10239", Channel.PHONE, Direction.INBOUND, "Déclaration d'hospitalisation",
                "Prise en charge demandée à la mutuelle partenaire.", 120);
        interaction(m, "C-10240", Channel.EMAIL, Direction.INBOUND, "Résiliation du contrat auto",
                "Vente du véhicule, résiliation enregistrée.", 260);
        interaction(m, "C-10241", Channel.MEETING, Direction.OUTBOUND, "Visite de la boulangerie",
                "Panne de la chambre froide : dossier sinistre en cours, perte d'exploitation à chiffrer.", 10);
        interaction(l, "C-10242", Channel.PHONE, Direction.INBOUND, "Changement d'adresse", null, 30);
        interaction(l, "C-10243", Channel.MEETING, Direction.OUTBOUND, "Point annuel famille", null, 200);
        interaction(l, "C-10245", Channel.VIDEO, Direction.OUTBOUND, "Renouvellement multirisque pro",
                "Ajout du nouveau matériel informatique au contrat.", 60);
        interaction(l, "C-10246", Channel.PHONE, Direction.INBOUND, "Devis pour une seconde voiture",
                "Devis envoyé, la cliente compare avec la concurrence.", 5);
        interaction(l, "C-10247", Channel.EMAIL, Direction.OUTBOUND, "Refus de prise en charge dentaire",
                "Motif expliqué : dépassement du plafond annuel.", 400);

        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);
        task(m.id(), "C-10234", "Proposer une assurance habitation", "Cliente propriétaire depuis peu, à recontacter.",
                Priority.MEDIUM, today.plusDays(3), RecommendationCode.HOME_CROSS_SELL.name(), "", TaskStatus.OPEN, now);
        task(m.id(), "C-10235", "Faire le point avec l'expert sur le dégât des eaux", null, Priority.HIGH, today,
                null, null, TaskStatus.OPEN, now);
        task(m.id(), "C-10241", "Envoyer l'attestation flotte mise à jour", "Demandée par le comptable du client.",
                Priority.HIGH, today.minusDays(2), null, null, TaskStatus.OPEN, now);
        task(m.id(), "C-10238", "Préparer la simulation épargne retraite", null, Priority.LOW, today.plusDays(10),
                null, null, TaskStatus.OPEN, now);
        task(m.id(), "C-10239", "Transmettre la facture d'hospitalisation", null, Priority.MEDIUM, today.minusDays(30),
                null, null, TaskStatus.DONE, now);
        task(l.id(), "C-10246", "Relancer pour le devis seconde voiture", null, Priority.MEDIUM, today.plusDays(2),
                null, null, TaskStatus.OPEN, now);
        task(l.id(), "C-10244", "Accueillir la nouvelle cliente (appel de bienvenue)", null, Priority.LOW, today.minusDays(1),
                null, null, TaskStatus.OPEN, now);

        dismissals.save(new Dismissal(new CustomerId("C-10243"), RecommendationCode.PROTECTION_CROSS_SELL, "", l.id(),
                "Client déjà couvert par la prévoyance de son employeur.", now.minusSeconds(86_400L * 20)));

        LOG.info("Données de démonstration créées : connectez-vous avec mckevin@demo.fr / {}", DEMO_PASSWORD);
    }

    private record Agent(long id, String name) {
    }

    private void interaction(Agent agent, String customerId, Channel channel, Direction direction, String subject,
                             String notes, int daysAgo) {
        Instant when = LocalDate.now(clock).minusDays(daysAgo).atTime(LocalTime.of(10, 30)).atZone(clock.getZone()).toInstant();
        interactions.save(new Interaction(null, new CustomerId(customerId), agent.id(), agent.name(), channel, direction,
                subject, notes, when));
    }

    @SuppressWarnings("java:S107")
    private void task(long assignee, String customerId, String title, String notes, Priority priority, LocalDate due,
                      String code, String reference, TaskStatus status, Instant now) {
        tasks.save(new FollowUpTask(null, new CustomerId(customerId), assignee, title, notes, priority, due, status, code,
                reference, now, status == TaskStatus.DONE ? now : null));
    }
}

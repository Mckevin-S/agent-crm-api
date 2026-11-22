package com.mckevin.agentcrm.bdd;

import com.mckevin.agentcrm.application.AccessDeniedException;
import com.mckevin.agentcrm.application.Agent;
import com.mckevin.agentcrm.application.CustomerService;
import com.mckevin.agentcrm.application.FollowUpService;
import com.mckevin.agentcrm.application.ProfileLoader;
import com.mckevin.agentcrm.application.RecommendationFinder;
import com.mckevin.agentcrm.application.Views.Customer360;
import com.mckevin.agentcrm.application.port.AgentDirectory.AgentSummary;
import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.domain.customer.Claim;
import com.mckevin.agentcrm.domain.customer.ClaimStatus;
import com.mckevin.agentcrm.domain.customer.Contract;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.customer.Segment;
import com.mckevin.agentcrm.domain.interaction.Channel;
import com.mckevin.agentcrm.domain.interaction.Direction;
import com.mckevin.agentcrm.domain.interaction.Interaction;
import com.mckevin.agentcrm.domain.recommendation.NextBestActionEngine;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
import com.mckevin.agentcrm.domain.shared.BusinessRuleViolation;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import com.mckevin.agentcrm.support.InMemoryAdapters;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Et;
import io.cucumber.java.fr.Quand;
import io.cucumber.java.fr.Soit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Étapes des scénarios : une agence complète montée avec les adaptateurs en mémoire. */
public class AgencySteps {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final long ADVISOR_ID = 1;
    private static final long COLLEAGUE_ID = 2;

    private final InMemoryAdapters.Crm crm = new InMemoryAdapters.Crm();
    private final InMemoryAdapters.Snapshots snapshots = new InMemoryAdapters.Snapshots();
    private final InMemoryAdapters.Interactions interactions = new InMemoryAdapters.Interactions();
    private final InMemoryAdapters.Tasks tasks = new InMemoryAdapters.Tasks();
    private final InMemoryAdapters.Dismissals dismissals = new InMemoryAdapters.Dismissals();
    private final InMemoryAdapters.Portfolio portfolio = new InMemoryAdapters.Portfolio();
    private final InMemoryAdapters.Agents agents = new InMemoryAdapters.Agents();

    private LocalDate today;
    private Clock clock;
    private Agent advisor;
    private CustomerId customerId;
    private Customer customer;
    private final List<Contract> contracts = new ArrayList<>();
    private final List<Claim> claims = new ArrayList<>();
    private CustomerService customerService;
    private FollowUpService followUpService;
    private Customer360 view;
    private RuntimeException failure;

    @Soit("nous sommes le {string}")
    public void nousSommesLe(String date) {
        today = LocalDate.parse(date);
        clock = Clock.fixed(today.atTime(9, 0).atZone(PARIS).toInstant(), PARIS);
        ProfileLoader loader = new ProfileLoader(crm, snapshots, clock);
        RecommendationFinder finder = new RecommendationFinder(interactions, tasks, dismissals,
                NextBestActionEngine.withDefaultRules(), clock);
        customerService = new CustomerService(crm, snapshots, loader, portfolio, agents, interactions, tasks, finder, clock);
        followUpService = new FollowUpService(customerService, loader, finder, interactions, tasks, dismissals, portfolio,
                snapshots, clock);
    }

    @Soit("le conseiller {string} gère le client {string} né le {string}")
    public void leConseillerGereLeClient(String name, String id, String birthDate) {
        advisor = new Agent(ADVISOR_ID, name, false);
        agents.add(new AgentSummary(ADVISOR_ID, name, "conseiller@test.fr", false));
        customerId = new CustomerId(id);
        customer = new Customer(customerId, "Mme", "Claire", "Martin", LocalDate.parse(birthDate), null, null, "Lille",
                "59000", Segment.INDIVIDUAL, LocalDate.of(2015, 1, 1));
        portfolio.assign(customerId, ADVISOR_ID);
        publishProfile();
    }

    @Soit("le client a les contrats suivants :")
    public void contratsSuivants(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            contracts.add(new Contract(row.get("numéro"), ProductLine.valueOf(row.get("produit")), row.get("produit"),
                    ContractStatus.valueOf(row.get("statut")), new BigDecimal("500"), today.minusYears(2),
                    today.plusDays(Integer.parseInt(row.get("échéance dans")))));
        }
        publishProfile();
    }

    @Et("le client a un sinistre {string} ouvert depuis {int} jours")
    public void unSinistre(String number, int days) {
        claims.add(new Claim(number, "MRH-1", "Dégât des eaux", ClaimStatus.OPEN, today.minusDays(days), new BigDecimal("1500")));
        publishProfile();
    }

    @Et("le dernier échange avec le client date de {int} jours")
    public void dernierEchange(int days) {
        interactions.save(new Interaction(null, customerId, ADVISOR_ID, advisor.fullName(), Channel.PHONE, Direction.INBOUND,
                "Échange", null, Instant.now(clock).minusSeconds(86_400L * days)));
    }

    @Soit("le conseiller a déjà ouvert la fiche du client")
    public void dejaOuvert() {
        customerService.view(advisor, customerId);
    }

    @Soit("le CRM tombe en panne")
    public void crmEnPanne() {
        crm.setDown(true);
    }

    @Soit("le client {string} est géré par un autre conseiller")
    public void gereParUnAutre(String id) {
        portfolio.assign(new CustomerId(id), COLLEAGUE_ID);
    }

    @Quand("le conseiller ouvre la fiche du client")
    public void ouvreLaFiche() {
        open(customerId);
    }

    @Quand("le conseiller ouvre la fiche du client {string}")
    public void ouvreLaFicheDe(String id) {
        open(new CustomerId(id));
    }

    @Quand("le conseiller trace un appel sortant {string}")
    public void traceUnAppel(String subject) {
        followUpService.logInteraction(advisor, customerId, new FollowUpService.LogInteractionCommand(Channel.PHONE,
                Direction.OUTBOUND, subject, null, null, null, null));
    }

    @Quand("le conseiller accepte la recommandation {string} sur {string}")
    public void accepte(String code, String reference) {
        attempt(() -> followUpService.accept(advisor, customerId, RecommendationCode.valueOf(code), reference, null));
    }

    @Quand("le conseiller écarte la recommandation {string} sans motif")
    public void ecarteSansMotif(String code) {
        attempt(() -> followUpService.dismiss(advisor, customerId, RecommendationCode.valueOf(code), "", " "));
    }

    @Quand("le conseiller écarte la recommandation {string} avec le motif {string}")
    public void ecarte(String code, String reason) {
        attempt(() -> followUpService.dismiss(advisor, customerId, RecommendationCode.valueOf(code), "", reason));
    }

    @Alors("la recommandation {string} sur {string} est proposée en priorité {string}")
    public void recommandationSurProposee(String code, String reference, String priority) {
        assertThat(currentRecommendations())
                .anySatisfy(r -> {
                    assertThat(r.matches(RecommendationCode.valueOf(code), reference)).isTrue();
                    assertThat(r.priority()).isEqualTo(Priority.valueOf(priority));
                });
    }

    @Alors("la recommandation {string} est proposée en priorité {string}")
    public void recommandationProposee(String code, String priority) {
        recommandationSurProposee(code, "", priority);
    }

    @Alors("la recommandation {string} n'est plus proposée")
    public void plusProposee(String code) {
        assertThat(currentRecommendations()).noneMatch(r -> r.code() == RecommendationCode.valueOf(code));
    }

    @Alors("une tâche ouverte {string} est assignée au conseiller")
    public void tacheOuverte(String title) {
        assertThat(tasks.findOpenByAssignee(ADVISOR_ID)).extracting(FollowUpTask::getTitle).contains(title);
    }

    @Alors("l'action est refusée avec le code {string}")
    public void actionRefusee(String code) {
        assertThat(failure).isInstanceOf(BusinessRuleViolation.class);
        assertThat(((BusinessRuleViolation) failure).code()).isEqualTo(code);
    }

    @Alors("la fiche est affichée à partir de la copie locale")
    public void copieLocale() {
        assertThat(failure).isNull();
        assertThat(view.live()).isFalse();
        assertThat(view.profile().customer().id()).isEqualTo(customerId);
    }

    @Alors("l'erreur {string} est signalée")
    public void erreurSignalee(String message) {
        assertThat(failure).isInstanceOf(CrmUnavailableException.class);
        assertThat(failure.getMessage()).contains(message);
    }

    @Alors("l'accès est refusé")
    public void accesRefuse() {
        assertThat(failure).isInstanceOf(AccessDeniedException.class);
    }

    /** Rouvre la fiche pour lire les recommandations à jour. */
    private List<Recommendation> currentRecommendations() {
        open(customerId);
        assertThat(failure).as("ouverture de la fiche").isNull();
        return view.recommendations();
    }

    private void open(CustomerId id) {
        attempt(() -> view = customerService.view(advisor, id));
    }

    private void attempt(Runnable action) {
        failure = null;
        try {
            action.run();
        } catch (RuntimeException e) {
            failure = e;
        }
    }

    private void publishProfile() {
        if (customer != null) {
            crm.put(new CustomerProfile(customer, contracts, claims));
        }
    }
}

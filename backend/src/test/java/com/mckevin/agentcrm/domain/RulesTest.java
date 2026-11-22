package com.mckevin.agentcrm.domain;

import com.mckevin.agentcrm.domain.customer.ClaimStatus;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.recommendation.CustomerSituation;
import com.mckevin.agentcrm.domain.recommendation.NextBestActionEngine;
import com.mckevin.agentcrm.domain.recommendation.Recommendation;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
import com.mckevin.agentcrm.domain.shared.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.mckevin.agentcrm.support.Fixtures.TODAY;
import static com.mckevin.agentcrm.support.Fixtures.claim;
import static com.mckevin.agentcrm.support.Fixtures.contract;
import static com.mckevin.agentcrm.support.Fixtures.individual;
import static com.mckevin.agentcrm.support.Fixtures.professional;
import static org.assertj.core.api.Assertions.assertThat;

class RulesTest {

    private static final Instant NOW = TODAY.atTime(9, 0).atZone(ZoneId.of("Europe/Paris")).toInstant();
    private static final Optional<Instant> RECENT_CONTACT = Optional.of(NOW.minusSeconds(86_400L * 10));
    private final NextBestActionEngine engine = NextBestActionEngine.withDefaultRules();

    private List<Recommendation> recommend(CustomerProfile profile, Optional<Instant> lastContact) {
        return engine.recommend(new CustomerSituation(profile, lastContact, TODAY), List.of(), List.of(), NOW);
    }

    private static List<RecommendationCode> codes(List<Recommendation> recommendations) {
        return recommendations.stream().map(Recommendation::code).toList();
    }

    @ParameterizedTest(name = "échéance dans {0} jours → {1}")
    @CsvSource({"0, HIGH", "15, HIGH", "16, MEDIUM", "45, MEDIUM"})
    void renewalWindow(int days, Priority expected) {
        CustomerProfile p = new CustomerProfile(professional("C-10001"),
                List.of(contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, days)), List.of());

        assertThat(recommend(p, RECENT_CONTACT)).singleElement().satisfies(r -> {
            assertThat(r.code()).isEqualTo(RecommendationCode.RENEWAL_SOON);
            assertThat(r.reference()).isEqualTo("AUT-1");
            assertThat(r.priority()).isEqualTo(expected);
        });
    }

    @Test
    @DisplayName("Pas de renouvellement au-delà de 45 jours, ni pour un contrat résilié")
    void noRenewalOutsideWindowOrForTerminatedContract() {
        CustomerProfile p = new CustomerProfile(professional("C-10001"), List.of(
                contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, 46),
                contract("AUT-2", ProductLine.AUTO, ContractStatus.TERMINATED, 5)), List.of());

        assertThat(recommend(p, RECENT_CONTACT)).isEmpty();
    }

    @Test
    void suspendedContractIsUrgent() {
        CustomerProfile p = new CustomerProfile(professional("C-10001"),
                List.of(contract("MRH-1", ProductLine.HOME, ContractStatus.SUSPENDED, 200)), List.of());

        assertThat(recommend(p, RECENT_CONTACT)).singleElement()
                .satisfies(r -> assertThat(r.code()).isEqualTo(RecommendationCode.SUSPENDED_CONTRACT))
                .satisfies(r -> assertThat(r.priority()).isEqualTo(Priority.HIGH));
    }

    @ParameterizedTest(name = "sinistre {0} ouvert depuis {1} jours → {2}")
    @CsvSource({"OPEN, 29, ", "OPEN, 30, MEDIUM", "IN_PROGRESS, 60, HIGH", "CLOSED, 90, ", "REJECTED, 90, "})
    void staleClaims(ClaimStatus status, int age, Priority expected) {
        CustomerProfile p = new CustomerProfile(professional("C-10001"), List.of(), List.of(claim("SIN-1", status, age)));

        List<Recommendation> result = recommend(p, RECENT_CONTACT);
        if (expected == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).singleElement().satisfies(r -> assertThat(r.priority()).isEqualTo(expected));
        }
    }

    @Test
    @DisplayName("Sans échange depuis plus de 180 jours, ou jamais, il faut reprendre contact")
    void noRecentContact() {
        CustomerProfile p = new CustomerProfile(professional("C-10001"), List.of(), List.of());

        assertThat(codes(recommend(p, Optional.empty()))).containsExactly(RecommendationCode.NO_RECENT_CONTACT);
        assertThat(codes(recommend(p, Optional.of(NOW.minusSeconds(86_400L * 181))))).containsExactly(RecommendationCode.NO_RECENT_CONTACT);
        assertThat(recommend(p, Optional.of(NOW.minusSeconds(86_400L * 179)))).isEmpty();
    }

    @Test
    @DisplayName("Particulier assuré en auto sans habitation : proposer une habitation")
    void homeCrossSell() {
        Customer claire = individual("C-10001", LocalDate.of(1995, 1, 1));
        CustomerProfile autoOnly = new CustomerProfile(claire,
                List.of(contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, 200)), List.of());
        CustomerProfile withHome = new CustomerProfile(claire, List.of(
                contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, 200),
                contract("MRH-1", ProductLine.HOME, ContractStatus.ACTIVE, 200)), List.of());

        assertThat(codes(recommend(autoOnly, RECENT_CONTACT))).containsExactly(RecommendationCode.HOME_CROSS_SELL);
        assertThat(codes(recommend(withHome, RECENT_CONTACT))).doesNotContain(RecommendationCode.HOME_CROSS_SELL);
    }

    @ParameterizedTest(name = "particulier de {0} ans, multi-équipé → prévoyance : {1}")
    @CsvSource({"29, false", "30, true", "60, true", "61, false"})
    void protectionCrossSellAgeRange(int age, boolean expected) {
        CustomerProfile p = new CustomerProfile(individual("C-10001", TODAY.minusYears(age)), List.of(
                contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, 200),
                contract("MRH-1", ProductLine.HOME, ContractStatus.ACTIVE, 200)), List.of());

        assertThat(codes(recommend(p, RECENT_CONTACT)).contains(RecommendationCode.PROTECTION_CROSS_SELL)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Les recommandations sont triées par priorité décroissante")
    void orderedByPriority() {
        CustomerProfile p = new CustomerProfile(individual("C-10001", LocalDate.of(1980, 1, 1)), List.of(
                contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, 30),
                contract("SAN-1", ProductLine.HEALTH, ContractStatus.ACTIVE, 300),
                contract("MRH-1", ProductLine.HOME, ContractStatus.SUSPENDED, 300)), List.of());

        assertThat(recommend(p, Optional.empty())).extracting(Recommendation::priority)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }
}

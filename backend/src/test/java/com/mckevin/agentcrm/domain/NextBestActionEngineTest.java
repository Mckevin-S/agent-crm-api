package com.mckevin.agentcrm.domain;

import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.recommendation.CustomerSituation;
import com.mckevin.agentcrm.domain.recommendation.Dismissal;
import com.mckevin.agentcrm.domain.recommendation.NextBestActionEngine;
import com.mckevin.agentcrm.domain.recommendation.RecommendationCode;
import com.mckevin.agentcrm.domain.shared.Priority;
import com.mckevin.agentcrm.domain.task.FollowUpTask;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.mckevin.agentcrm.support.Fixtures.TODAY;
import static com.mckevin.agentcrm.support.Fixtures.contract;
import static com.mckevin.agentcrm.support.Fixtures.professional;
import static org.assertj.core.api.Assertions.assertThat;

class NextBestActionEngineTest {

    private static final Instant NOW = TODAY.atTime(9, 0).atZone(ZoneId.of("Europe/Paris")).toInstant();
    private static final CustomerId ID = new CustomerId("C-10001");
    private final NextBestActionEngine engine = NextBestActionEngine.withDefaultRules();
    private final CustomerSituation situation = new CustomerSituation(new CustomerProfile(professional("C-10001"),
            List.of(contract("AUT-1", ProductLine.AUTO, ContractStatus.ACTIVE, 10)), List.of()),
            Optional.of(NOW.minusSeconds(86_400)), TODAY);

    @Test
    void dismissedRecommendationIsHiddenFor90Days() {
        Dismissal recent = new Dismissal(ID, RecommendationCode.RENEWAL_SOON, "AUT-1", 1, "Déjà traité", NOW.minusSeconds(86_400L * 89));
        Dismissal expired = new Dismissal(ID, RecommendationCode.RENEWAL_SOON, "AUT-1", 1, "Déjà traité", NOW.minusSeconds(86_400L * 91));

        assertThat(engine.recommend(situation, List.of(recent), List.of(), NOW)).isEmpty();
        assertThat(engine.recommend(situation, List.of(expired), List.of(), NOW)).hasSize(1);
    }

    @Test
    void dismissalOnAnotherContractDoesNotHide() {
        Dismissal other = new Dismissal(ID, RecommendationCode.RENEWAL_SOON, "AUT-2", 1, "Autre", NOW);

        assertThat(engine.recommend(situation, List.of(other), List.of(), NOW)).hasSize(1);
    }

    @Test
    void openTaskFromTheRecommendationHidesIt() {
        FollowUpTask open = FollowUpTask.open(ID, 1, "Renouvellement", null, Priority.HIGH, TODAY.plusDays(2),
                RecommendationCode.RENEWAL_SOON.name(), "AUT-1", TODAY, NOW);
        assertThat(engine.recommend(situation, List.of(), List.of(open), NOW)).isEmpty();

        open.complete(NOW);
        assertThat(engine.recommend(situation, List.of(), List.of(open), NOW)).hasSize(1);
    }
}

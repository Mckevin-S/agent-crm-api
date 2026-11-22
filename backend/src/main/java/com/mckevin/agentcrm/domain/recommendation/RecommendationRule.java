package com.mckevin.agentcrm.domain.recommendation;

import java.util.List;

/** Une règle métier qui propose zéro, une ou plusieurs actions pour un client. */
@FunctionalInterface
public interface RecommendationRule {

    List<Recommendation> evaluate(CustomerSituation situation);
}

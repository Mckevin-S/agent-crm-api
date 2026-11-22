package com.mckevin.agentcrm.domain.recommendation;

import com.mckevin.agentcrm.domain.task.FollowUpTask;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Calcule les prochaines actions conseillées pour un client : applique les règles, puis retire
 * celles que le conseiller a écartées récemment ou qui ont déjà une tâche ouverte.
 */
public class NextBestActionEngine {

    private static final Comparator<Recommendation> ORDER = Comparator
            .comparing(Recommendation::priority).reversed()
            .thenComparing(Recommendation::suggestedDueDate)
            .thenComparing(Recommendation::title);

    private final List<RecommendationRule> rules;

    public NextBestActionEngine(List<RecommendationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static NextBestActionEngine withDefaultRules() {
        return new NextBestActionEngine(Rules.all());
    }

    public List<Recommendation> recommend(CustomerSituation situation, List<Dismissal> dismissals,
                                          List<FollowUpTask> tasks, Instant now) {
        return rules.stream()
                .flatMap(rule -> rule.evaluate(situation).stream())
                .filter(r -> dismissals.stream().noneMatch(d -> d.isActiveAt(now) && r.matches(d.code(), d.reference())))
                .filter(r -> tasks.stream().noneMatch(t -> t.isOpen() && t.comesFrom(r.code().name(), r.reference())))
                .sorted(ORDER)
                .toList();
    }
}

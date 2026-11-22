package com.mckevin.agentcrm.domain.recommendation;

import com.mckevin.agentcrm.domain.customer.Claim;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.CustomerProfile;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.customer.Segment;
import com.mckevin.agentcrm.domain.shared.Dates;
import com.mckevin.agentcrm.domain.shared.Priority;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Les règles de recommandation du portefeuille. Chaque seuil est une constante nommée et testée. */
public final class Rules {

    public static final int RENEWAL_WINDOW_DAYS = 45;
    public static final int URGENT_RENEWAL_DAYS = 15;
    public static final int STALE_CLAIM_DAYS = 30;
    public static final int VERY_STALE_CLAIM_DAYS = 60;
    public static final int NO_CONTACT_DAYS = 180;
    public static final int PROTECTION_MIN_AGE = 30;
    public static final int PROTECTION_MAX_AGE = 60;

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Rules() {
    }

    public static List<RecommendationRule> all() {
        return List.of(Rules::renewalSoon, Rules::suspendedContract, Rules::staleClaim, Rules::noRecentContact,
                Rules::homeCrossSell, Rules::protectionCrossSell);
    }

    /** Contrat en vigueur dont l'échéance tombe dans les 45 jours : préparer le renouvellement. */
    static List<Recommendation> renewalSoon(CustomerSituation s) {
        LocalDate today = s.today();
        return s.profile().activeContracts().stream()
                .filter(c -> {
                    long days = c.daysUntilRenewal(today);
                    return days >= 0 && days <= RENEWAL_WINDOW_DAYS;
                })
                .map(c -> {
                    long days = c.daysUntilRenewal(today);
                    Priority priority = days <= URGENT_RENEWAL_DAYS ? Priority.HIGH : Priority.MEDIUM;
                    return new Recommendation(RecommendationCode.RENEWAL_SOON, c.number(), priority,
                            "Préparer le renouvellement – " + c.product().label() + " " + c.number(),
                            "Échéance le " + Dates.fr(c.renewalDate()) + ", dans " + days + " jour" + (days > 1 ? "s" : "")
                                    + ". Revoir les garanties et la prime avant la date anniversaire.",
                            today.plusDays(Math.max(1, Math.min(7, days - 2))));
                })
                .toList();
    }

    /** Contrat suspendu (impayé, pièce manquante) : à régulariser en priorité. */
    static List<Recommendation> suspendedContract(CustomerSituation s) {
        return s.profile().contracts().stream()
                .filter(c -> c.status() == ContractStatus.SUSPENDED)
                .map(c -> new Recommendation(RecommendationCode.SUSPENDED_CONTRACT, c.number(), Priority.HIGH,
                        "Régulariser le contrat suspendu " + c.number(),
                        "Le contrat " + c.label() + " est suspendu : le client n'est plus couvert.",
                        s.today().plusDays(1)))
                .toList();
    }

    /** Sinistre ouvert depuis plus de 30 jours : faire le point avec le client. */
    static List<Recommendation> staleClaim(CustomerSituation s) {
        LocalDate today = s.today();
        return s.profile().pendingClaims().stream()
                .filter(c -> c.ageInDays(today) >= STALE_CLAIM_DAYS)
                .map(c -> claimRecommendation(c, today))
                .toList();
    }

    private static Recommendation claimRecommendation(Claim claim, LocalDate today) {
        long age = claim.ageInDays(today);
        Priority priority = age >= VERY_STALE_CLAIM_DAYS ? Priority.HIGH : Priority.MEDIUM;
        return new Recommendation(RecommendationCode.STALE_CLAIM, claim.number(), priority,
                "Faire le point sur le sinistre " + claim.number(),
                claim.nature() + " ouvert depuis " + age + " jours (" + claim.status().label().toLowerCase() + ").",
                today.plusDays(2));
    }

    /** Aucun échange depuis 6 mois : reprendre contact. */
    static List<Recommendation> noRecentContact(CustomerSituation s) {
        boolean stale = s.lastInteraction()
                .map(last -> ChronoUnit.DAYS.between(last.atZone(ZONE).toLocalDate(), s.today()) > NO_CONTACT_DAYS)
                .orElse(true);
        if (!stale) {
            return List.of();
        }
        String rationale = s.lastInteraction()
                .map(last -> "Dernier échange le " + Dates.fr(last.atZone(ZONE).toLocalDate()) + ".")
                .orElse("Aucun échange enregistré avec ce client.");
        return List.of(new Recommendation(RecommendationCode.NO_RECENT_CONTACT, "", Priority.MEDIUM,
                "Reprendre contact avec le client", rationale + " Un point annuel entretient la relation.",
                s.today().plusDays(7)));
    }

    /** Particulier assuré en auto mais pas en habitation : proposer une habitation. */
    static List<Recommendation> homeCrossSell(CustomerSituation s) {
        CustomerProfile p = s.profile();
        if (p.customer().segment() != Segment.INDIVIDUAL || !p.holdsActive(ProductLine.AUTO) || p.holdsActive(ProductLine.HOME)) {
            return List.of();
        }
        return List.of(new Recommendation(RecommendationCode.HOME_CROSS_SELL, "", Priority.MEDIUM,
                "Proposer une assurance habitation",
                "Client assuré en auto sans contrat habitation chez nous : opportunité d'équipement.",
                s.today().plusDays(14)));
    }

    /** Particulier de 30 à 60 ans, au moins deux contrats, sans prévoyance : proposer une prévoyance. */
    static List<Recommendation> protectionCrossSell(CustomerSituation s) {
        CustomerProfile p = s.profile();
        int age = p.customer().ageOn(s.today());
        if (p.customer().segment() != Segment.INDIVIDUAL || age < PROTECTION_MIN_AGE || age > PROTECTION_MAX_AGE
                || p.activeContracts().size() < 2 || p.holdsActive(ProductLine.PROTECTION)) {
            return List.of();
        }
        return List.of(new Recommendation(RecommendationCode.PROTECTION_CROSS_SELL, "", Priority.LOW,
                "Proposer une prévoyance",
                "Client fidèle de " + age + " ans, multi-équipé, sans garantie prévoyance.",
                s.today().plusDays(30)));
    }
}

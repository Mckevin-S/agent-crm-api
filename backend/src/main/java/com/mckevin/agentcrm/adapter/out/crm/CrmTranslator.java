package com.mckevin.agentcrm.adapter.out.crm;

import com.mckevin.agentcrm.domain.customer.Claim;
import com.mckevin.agentcrm.domain.customer.ClaimStatus;
import com.mckevin.agentcrm.domain.customer.Contract;
import com.mckevin.agentcrm.domain.customer.ContractStatus;
import com.mckevin.agentcrm.domain.customer.Customer;
import com.mckevin.agentcrm.domain.customer.CustomerId;
import com.mckevin.agentcrm.domain.customer.ProductLine;
import com.mckevin.agentcrm.domain.customer.Segment;

import java.math.BigDecimal;

/**
 * Couche anticorruption : traduit les codes et le vocabulaire du CRM (MRH, EN_VIGUEUR, PART...)
 * vers le modèle du domaine. Un code inconnu est une erreur explicite, jamais une valeur devinée.
 */
final class CrmTranslator {

    private CrmTranslator() {
    }

    static Customer customer(CrmDtos.ClientDto c) {
        return new Customer(new CustomerId(c.numClient()), c.civilite(), c.prenom(), c.nom(), c.dateNaissance(), c.email(),
                c.telephone(), c.ville(), c.codePostal(), segment(c.segment()), c.clientDepuis());
    }

    static Contract contract(CrmDtos.ContratDto c) {
        return new Contract(c.numContrat(), product(c.produit()), c.libelle(), contractStatus(c.statut()),
                c.primeAnnuelle() == null ? BigDecimal.ZERO : c.primeAnnuelle(), c.dateEffet(), c.dateEcheance());
    }

    static Claim claim(CrmDtos.SinistreDto s) {
        return new Claim(s.numSinistre(), s.numContrat(), s.nature(), claimStatus(s.statut()), s.dateOuverture(),
                s.montantEstime());
    }

    static ProductLine product(String code) {
        return switch (code) {
            case "AUT" -> ProductLine.AUTO;
            case "MRH" -> ProductLine.HOME;
            case "SAN" -> ProductLine.HEALTH;
            case "PRV" -> ProductLine.PROTECTION;
            case "EPA" -> ProductLine.SAVINGS;
            default -> throw new IllegalArgumentException("Code produit CRM inconnu : " + code);
        };
    }

    static ContractStatus contractStatus(String code) {
        return switch (code) {
            case "EN_VIGUEUR" -> ContractStatus.ACTIVE;
            case "SUSPENDU" -> ContractStatus.SUSPENDED;
            case "RESILIE" -> ContractStatus.TERMINATED;
            default -> throw new IllegalArgumentException("Statut de contrat CRM inconnu : " + code);
        };
    }

    static ClaimStatus claimStatus(String code) {
        return switch (code) {
            case "OUVERT" -> ClaimStatus.OPEN;
            case "EN_COURS" -> ClaimStatus.IN_PROGRESS;
            case "CLOS" -> ClaimStatus.CLOSED;
            case "REFUSE" -> ClaimStatus.REJECTED;
            default -> throw new IllegalArgumentException("Statut de sinistre CRM inconnu : " + code);
        };
    }

    static Segment segment(String code) {
        return switch (code) {
            case "PART" -> Segment.INDIVIDUAL;
            case "PRO" -> Segment.PROFESSIONAL;
            default -> throw new IllegalArgumentException("Segment CRM inconnu : " + code);
        };
    }
}

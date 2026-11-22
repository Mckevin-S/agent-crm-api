package com.mckevin.agentcrm.adapter.out.crm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Format des réponses du CRM, avec son propre vocabulaire. Ces classes ne sortent jamais de l'adaptateur. */
final class CrmDtos {

    private CrmDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ClientDto(String numClient, String civilite, String nom, String prenom, LocalDate dateNaissance, String email,
                     String telephone, String ville, String codePostal, String segment, LocalDate clientDepuis) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ContratDto(String numContrat, String numClient, String produit, String libelle, String statut,
                      BigDecimal primeAnnuelle, LocalDate dateEffet, LocalDate dateEcheance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SinistreDto(String numSinistre, String numContrat, String numClient, String nature, String statut,
                       LocalDate dateOuverture, BigDecimal montantEstime) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ChaosDto(double failureRate, int latencyMs) {
    }
}

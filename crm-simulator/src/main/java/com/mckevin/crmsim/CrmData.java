package com.mckevin.crmsim;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Données du CRM, chargées depuis data/crm.json. Les dates de contrats et de sinistres sont exprimées
 * relativement à aujourd'hui, pour que la démonstration reste réaliste quel que soit le jour.
 */
@Component
public class CrmData {

    public record Client(String numClient, String civilite, String nom, String prenom, LocalDate dateNaissance,
                         String email, String telephone, String ville, String codePostal, String segment,
                         LocalDate clientDepuis) {
    }

    public record ContratSource(String numContrat, String numClient, String produit, String libelle, String statut,
                                BigDecimal primeAnnuelle, int dateEffetJours, int echeanceJours) {
    }

    public record SinistreSource(String numSinistre, String numContrat, String numClient, String nature, String statut,
                                 int ouvertIlYAJours, BigDecimal montantEstime) {
    }

    public record Contrat(String numContrat, String numClient, String produit, String libelle, String statut,
                          BigDecimal primeAnnuelle, LocalDate dateEffet, LocalDate dateEcheance) {
    }

    public record Sinistre(String numSinistre, String numContrat, String numClient, String nature, String statut,
                           LocalDate dateOuverture, BigDecimal montantEstime) {
    }

    record Dataset(List<Client> clients, List<ContratSource> contrats, List<SinistreSource> sinistres) {
    }

    private final Dataset data;
    private final Clock clock;

    public CrmData(Clock clock) {
        this.clock = clock;
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource("data/crm.json").getInputStream()) {
            this.data = mapper.readValue(in, Dataset.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger data/crm.json", e);
        }
    }

    public List<Client> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return data.clients().stream()
                .filter(c -> q.isEmpty()
                        || (c.nom() + " " + c.prenom() + " " + c.numClient() + " " + c.email()).toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing(Client::nom).thenComparing(Client::prenom))
                .toList();
    }

    public Optional<Client> client(String numClient) {
        return data.clients().stream().filter(c -> c.numClient().equals(numClient)).findFirst();
    }

    public List<Contrat> contrats(String numClient) {
        LocalDate today = LocalDate.now(clock);
        return data.contrats().stream()
                .filter(c -> c.numClient().equals(numClient))
                .map(c -> new Contrat(c.numContrat(), c.numClient(), c.produit(), c.libelle(), c.statut(), c.primeAnnuelle(),
                        today.plusDays(c.dateEffetJours()), today.plusDays(c.echeanceJours())))
                .toList();
    }

    public List<Sinistre> sinistres(String numClient) {
        LocalDate today = LocalDate.now(clock);
        return data.sinistres().stream()
                .filter(s -> s.numClient().equals(numClient))
                .map(s -> new Sinistre(s.numSinistre(), s.numContrat(), s.numClient(), s.nature(), s.statut(),
                        today.minusDays(s.ouvertIlYAJours()), s.montantEstime()))
                .toList();
    }
}

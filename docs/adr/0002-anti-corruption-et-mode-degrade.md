# ADR 0002 – Couche anti-corruption et mode dégradé

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le CRM historique expose ses propres codes (`AUT`, `MRH`, `EN_VIGUEUR`, `PART`...) et des champs en snake_case. Il peut être lent ou indisponible. Un conseiller au téléphone avec un client ne peut pas se retrouver devant un écran d'erreur.

## Décision

1. **Traduction** : `CrmTranslator` convertit les réponses du CRM en objets du domaine. Un code inconnu est rejeté explicitement plutôt qu'interprété au hasard. Aucun type du CRM ne sort de `adapter.out.crm`.
2. **Résilience** : chaque appel passe par une nouvelle tentative puis un disjoncteur Resilience4j. Une réponse 404 signifie « client inconnu » et ne compte pas comme une panne.
3. **Repli** : chaque fiche lue est conservée en copie locale. Si le CRM ne répond pas, la fiche est servie depuis cette copie, marquée `live: false` avec sa date. L'interface affiche alors un bandeau et l'indicateur « Mode dégradé ».
4. **Démonstration** : le manager peut simuler une panne depuis l'application (`CrmChaosAdapter`), et le CRM simulé accepte une injection de pannes aléatoires.

## Conséquences

- L'application reste utilisable pendant une panne : consultation des fiches déjà vues, échanges et tâches.
- Une fiche jamais consultée n'est pas disponible pendant la panne (réponse 503) ; ce cas est décrit dans un scénario Cucumber.
- Les données affichées en mode dégradé peuvent être anciennes : la date de la copie est toujours affichée.

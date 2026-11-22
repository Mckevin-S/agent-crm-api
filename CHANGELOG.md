# Changelog

Toutes les évolutions notables du projet sont consignées ici.
Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versionnage sémantique](https://semver.org/lang/fr/).

## [1.0.0] – 2026-11-22

### Ajouté

- CRM historique simulé : clients, contrats et sinistres dans son propre vocabulaire, injection de pannes et de latence.
- Domaine métier : fiche client, contrats, sinistres, échanges, tâches de suivi.
- Moteur de recommandations : renouvellement, contrat suspendu, sinistre qui traîne, client sans contact, propositions habitation et prévoyance.
- Suivi : accepter une recommandation (création de tâche), l'écarter avec un motif pour 90 jours, tracer un échange avec relance, terminer ou reporter une tâche.
- Cloisonnement par portefeuille, vue manager de l'agence et de la charge de l'équipe.
- Couche anti-corruption, disjoncteur et mode dégradé sur copie locale ; simulation de panne par le manager.
- API REST documentée (OpenAPI), authentification JWT, erreurs RFC 7807.
- Interface « Relais » : tableau de bord, recherche globale, fiche 360°, tâches, équipe, thème clair et sombre.
- Données de démonstration : trois comptes et quatorze clients.
- Tests : unitaires, scénarios Cucumber en français, règles ArchUnit, intégration Testcontainers, front-end Karma, bout en bout Playwright.
- Docker Compose et CI GitHub Actions.

### Corrigé

- Calcul des initiales du client dans les réponses de l'API.
- État du CRM resté « indisponible » après la fin d'une panne simulée.

[1.0.0]: https://github.com/Mckevin-S/agent-crm-api/releases/tag/v1.0.0

# Agent CRM API

[![CI](https://github.com/Mckevin-S/agent-crm-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Mckevin-S/agent-crm-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6db33f)
![Angular 17](https://img.shields.io/badge/Angular-17-dd0031)

Poste de travail du conseiller d'une agence d'assurance. Il réunit sur une seule fiche ce que le CRM historique éparpille : contrats, sinistres, échanges et tâches de suivi. Un moteur de règles propose chaque jour les actions qui comptent : un renouvellement à préparer, un contrat suspendu, un sinistre qui traîne, un client sans nouvelles, une garantie manquante. Le conseiller transforme une recommandation en tâche ou l'écarte avec un motif. Si le CRM tombe en panne, l'application continue de fonctionner en mode dégradé à partir de la dernière copie connue.

> Projet personnel de démonstration. L'interface s'appelle « Relais » ; le CRM est simulé et les données sont fictives.

## Fonctionnalités

- **Fiche client 360°** : identité, conseiller attitré, primes annuelles, contrats avec leur échéance, sinistres, tâches ouvertes et historique des échanges.
- **Actions recommandées** (« next best action »), triées par priorité :
  - renouvellement à préparer (échéance dans les 45 jours) ;
  - contrat suspendu à régulariser ;
  - sinistre ouvert depuis plus de 30 jours ;
  - client sans contact depuis plus de six mois ;
  - proposition d'assurance habitation ou de prévoyance selon le profil.
- **Suivi** :
  - une recommandation acceptée devient une tâche avec une échéance suggérée ;
  - une recommandation écartée exige un motif et reste masquée 90 jours ;
  - échanges tracés (téléphone, e-mail, rendez-vous, visio) avec relance facultative ;
  - tâches regroupées en retard / aujourd'hui / à venir, report d'échéance.
- **Tableau de bord** du portefeuille : renouvellements à 30 jours, sinistres en cours, tâches en retard.
- **Rôles** : un conseiller ne voit que son portefeuille ; le manager d'agence voit toute l'agence et la charge de chaque conseiller.
- **Mode dégradé** : quand le CRM ne répond plus, un disjoncteur coupe les appels, les fiches sont servies depuis une copie locale et l'interface le signale clairement. Le manager peut simuler une panne depuis l'application pour le démontrer.
- **Recherche globale** par nom, numéro client, e-mail ou ville.
- **Thème clair et sombre**.

## Stack

| Couche | Technologies |
|---|---|
| Back-end | Java 21, Spring Boot 3.3 (Web, Validation, Data JPA, Security, OAuth2 Resource Server), Resilience4j, Flyway, springdoc-openapi |
| CRM simulé | Spring Boot 3.3, données JSON, injection de pannes et de latence |
| Base de données | PostgreSQL 16 |
| Front-end | Angular 17 (composants autonomes, signaux, nouveau control flow), Angular Material, Chart.js |
| Qualité | JUnit 5, AssertJ, Cucumber (Gherkin en français), ArchUnit, MockRestServiceServer, Testcontainers, JaCoCo, Jasmine/Karma, ESLint, Playwright |
| Livraison | Docker, Docker Compose, nginx, GitHub Actions |

## Architecture

L'API suit une architecture hexagonale (ports et adaptateurs). Le domaine ne dépend ni de Spring ni de la base de données ; ArchUnit le vérifie à chaque build.

```
backend/src/main/java/com/mckevin/agentcrm
├── domain/          Modèle métier pur : client, contrat, sinistre, échange, tâche, règles de recommandation
├── application/     Cas d'usage (fiche client, suivi, tableau de bord) et ports
│   └── port/        Interfaces : CrmGateway, TaskRepository, ProfileSnapshots...
├── adapter/
│   ├── in/web/      Contrôleurs REST, DTO, erreurs RFC 7807
│   └── out/
│       ├── crm/          Client HTTP du CRM, couche anti-corruption, disjoncteur
│       └── persistence/  JPA : échanges, tâches, portefeuilles, copies locales des fiches
└── config/          Sécurité JWT, assemblage de l'hexagone, horloge

crm-simulator/       CRM historique simulé (port 8090), avec son propre vocabulaire

frontend/src/app
├── core/            Modèles, client API typé, session, état du CRM
├── shared/          Composants réutilisables (avatar, graphique, état vide)
└── features/        Accueil, clients, fiche client, tâches, équipe
```

Le CRM parle sa propre langue : codes produit `AUT`, `MRH`, `PRV`, statuts `EN_VIGUEUR` ou `SUSPENDU`, champs en snake_case. Un traducteur dédié (`CrmTranslator`) convertit ces données en objets du domaine ; aucun terme du CRM ne dépasse l'adaptateur.

Chaque appel au CRM passe par un disjoncteur et une politique de nouvelle tentative (Resilience4j). Chaque fiche lue est conservée en copie locale. Si le CRM est indisponible, la fiche est servie depuis cette copie et marquée `live: false`. Les échanges et les tâches, stockés dans la base de l'application, restent modifiables.

Plus de détails : [docs/architecture.md](docs/architecture.md) et les décisions d'architecture dans [docs/adr](docs/adr).

### Règles métier

1. Un contrat en vigueur dont l'échéance tombe dans les 45 jours déclenche un renouvellement à préparer, en priorité haute à 15 jours ou moins.
2. Un contrat suspendu déclenche une régularisation en priorité haute.
3. Un sinistre ouvert depuis 30 jours ou plus déclenche une relance, en priorité haute à partir de 60 jours.
4. Un client sans échange depuis plus de 180 jours déclenche une reprise de contact.
5. Un particulier assuré en auto sans assurance habitation se voit proposer une habitation.
6. Un particulier de 30 à 60 ans avec au moins deux contrats actifs et sans prévoyance se voit proposer une prévoyance.
7. Une recommandation qui a déjà une tâche ouverte n'est plus proposée.
8. Écarter une recommandation exige un motif ; elle reste masquée 90 jours.
9. Une tâche ne peut pas avoir une échéance passée ; seule une tâche ouverte peut être terminée ou reportée.
10. Un conseiller n'accède qu'aux clients de son portefeuille ; le manager accède à toute l'agence.

## API

Documentation interactive : http://localhost:8080/swagger-ui.html (bouton *Authorize* avec le jeton obtenu à la connexion).

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/auth/login` | Se connecte, renvoie un jeton JWT |
| GET | `/api/auth/me` | Conseiller connecté |
| GET | `/api/customers?q=` | Recherche dans le portefeuille |
| GET | `/api/customers/{id}` | Fiche 360° avec recommandations |
| POST | `/api/customers/{id}/interactions` | Tracer un échange, avec relance facultative |
| POST | `/api/customers/{id}/tasks` | Créer une tâche |
| POST | `/api/customers/{id}/recommendations/{code}/accept` | Transformer une recommandation en tâche |
| POST | `/api/customers/{id}/recommendations/{code}/dismiss` | Écarter une recommandation avec un motif |
| GET | `/api/tasks` | Mes tâches (toute l'agence pour le manager) |
| POST | `/api/tasks/{id}/complete` | Terminer une tâche |
| PUT | `/api/tasks/{id}/due-date` | Reporter l'échéance |
| GET | `/api/dashboard` | Indicateurs et actions du portefeuille |
| GET | `/api/team` | Charge par conseiller (manager) |
| GET | `/api/crm/status` | État du CRM et du disjoncteur |
| PUT | `/api/crm/outage` | Simuler ou lever une panne du CRM (manager) |

Exemple :

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"mckevin@demo.fr","password":"demo1234"}' | jq -r .token)

curl -X POST http://localhost:8080/api/customers/C-10234/recommendations/RENEWAL_SOON/accept \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"reference":"AUT-558201"}'
```

Les erreurs suivent le format Problem Details (RFC 7807) :

| Code | Cas |
|---|---|
| 400 | Champ invalide |
| 401 | Jeton absent ou expiré |
| 403 | Client hors du portefeuille ou action réservée au manager |
| 404 | Client ou tâche introuvable |
| 422 | Règle métier non respectée, avec un `code` stable (`DUE_DATE_IN_PAST`, `REASON_REQUIRED`, `TASK_NOT_OPEN`...) |
| 503 | CRM indisponible et aucune copie locale de la fiche (`CRM_UNAVAILABLE`) |

## Lancer le projet

### Avec Docker (le plus simple)

```bash
docker compose up --build
```

- Application : http://localhost:4200
- API et Swagger : http://localhost:8080/swagger-ui.html
- CRM simulé : http://localhost:8090/crm/v1/clients

Les données de démonstration sont chargées au premier démarrage : une agence de trois personnes et quatorze clients. Les boutons « Explorer la démo » de l'écran de connexion connectent directement avec l'un de ces comptes :

| Compte | Rôle |
|---|---|
| `mckevin@demo.fr` | Conseiller (clients C-10234 à C-10241) |
| `lucas@demo.fr` | Conseiller (clients C-10242 à C-10247) |
| `sophie@demo.fr` | Manager d'agence |

Mot de passe commun : `demo1234`. Pour démarrer sans données de démonstration : `DEMO_DATA=false docker compose up --build`.

Pour observer le mode dégradé, connectez-vous en manager, ouvrez une fiche client, puis activez « Simuler une panne du CRM » depuis l'indicateur en haut à droite. Le CRM simulé accepte aussi une injection de pannes aléatoires :

```bash
curl -X PUT http://localhost:8090/admin/chaos -H "Content-Type: application/json" \
  -d '{"failure_rate":0.5,"latency_ms":800}'
```

### En développement

Prérequis : Java 21, Maven 3.9, Node 20, Docker (pour PostgreSQL et les tests d'intégration).

```bash
# Base de données
docker compose up -d db

# CRM simulé
cd crm-simulator
mvn spring-boot:run

# Back-end (données de démonstration activées)
cd backend
DEMO_DATA=true mvn spring-boot:run

# Front-end (proxy /api → localhost:8080)
cd frontend
npm install
npm start
```

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL | `jdbc:postgresql://localhost:5432/agentcrm`, `agentcrm`, `agentcrm` |
| `CRM_BASE_URL` | Adresse du CRM | `http://localhost:8090` |
| `JWT_SECRET` | Clé de signature des jetons (32 caractères minimum) | valeur de développement |
| `JWT_VALIDITY` | Durée de validité des jetons | `PT8H` |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées | `http://localhost:4200` |
| `DEMO_DATA` | Charge les données de démonstration sur une base vide | `false` (`true` avec Docker Compose) |

## Tests

```bash
cd crm-simulator && mvn verify
cd backend && mvn verify                # unitaires, BDD, architecture, intégration sur PostgreSQL (Docker requis)
cd frontend && npm run lint && npm run test:ci
cd frontend && npm run e2e              # bout en bout, application lancée avec docker compose
```

- **Domaine** :
  - `RulesTest` : chaque règle de recommandation, seuils et priorités compris ;
  - `NextBestActionEngineTest` : tri, recommandations écartées, tâches déjà ouvertes ;
  - `FollowUpTaskTest` : cycle de vie d'une tâche.
- **Comportement (Cucumber, en français)** : les scénarios de `src/test/resources/features` décrivent les recommandations, le suivi et le mode dégradé dans le vocabulaire du métier. Ils s'exécutent sur les cas d'usage avec des adaptateurs en mémoire, sans Spring ni base de données. Rapport HTML : `backend/target/cucumber-report.html`.
- **Architecture (ArchUnit)** : le domaine ne dépend d'aucun framework, l'application ne connaît pas les adaptateurs, les adaptateurs ne se connaissent pas entre eux.
- **Adaptateur CRM** : traduction du vocabulaire du CRM, client introuvable, code produit inconnu, ouverture du disjoncteur après des échecs répétés (`MockRestServiceServer`).
- **Intégration (Testcontainers PostgreSQL)** : `AgencyApiIT` parcourt l'API complète, droits par rôle et mode dégradé compris.
- **Front-end** : session et intercepteur, état du CRM et mode dégradé, regroupement des tâches, formats.
- **Bout en bout (Playwright)** : parcours conseiller et manager sur l'application complète, panne du CRM comprise.

La CI GitHub Actions exécute toutes ces suites à chaque push et sur chaque pull request. Les rapports JaCoCo et Cucumber sont publiés en artefacts.

## Organisation du dépôt

Le développement suit un Git Flow simplifié :
- `main` contient les versions livrées et taguées ;
- `develop` intègre la prochaine version ;
- les branches `feature/ACR-<n>-<sujet>`, `fix/...` et `chore/...` partent de `develop` et y reviennent après une CI verte ;
- les messages de commit suivent Conventional Commits.

Le détail figure dans [CONTRIBUTING.md](CONTRIBUTING.md), les versions dans [CHANGELOG.md](CHANGELOG.md).

## Pistes d'évolution

- Synchronisation des fiches par événements (Kafka) plutôt qu'à la lecture
- Règles de recommandation paramétrables par l'agence
- Agenda partagé et prise de rendez-vous
- Authentification déléguée à un fournisseur OIDC (Keycloak)
- Indicateurs de conversion des recommandations par conseiller

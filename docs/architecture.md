# Architecture

## Vue d'ensemble

```
┌──────────────┐  HTTP/JSON   ┌───────────────────────────────────────────────┐   HTTP/JSON   ┌──────────────┐
│  Angular 17  │ ───────────▶ │                 API (hexagone)                │ ────────────▶ │  CRM simulé  │
│   « Relais » │   JWT        │                                               │  disjoncteur  │  (port 8090) │
└──────────────┘              │  adapter.in.web ─▶ application ─▶ domain      │               └──────────────┘
                              │                        │ ports                │
                              │                        ▼                      │
                              │  adapter.out.crm   adapter.out.persistence    │
                              └───────────────────────────────┬───────────────┘
                                                              ▼
                                                      ┌──────────────┐
                                                      │ PostgreSQL 16│
                                                      └──────────────┘
```

Le CRM reste la source de vérité pour les clients, les contrats et les sinistres. L'application possède ses propres données : portefeuilles, échanges, tâches, recommandations écartées et copies locales des fiches.

## Couches

| Couche | Contenu | Dépend de |
|---|---|---|
| `domain` | `Customer`, `Contract`, `Claim`, `Interaction`, `FollowUpTask`, règles de recommandation, `NextBestActionEngine` | rien (Java pur) |
| `application` | Cas d'usage (`CustomerService`, `FollowUpService`, `DashboardService`, `CrmService`, `AuthenticationService`) et ports | `domain` |
| `adapter.in.web` | Contrôleurs REST, DTO, `ApiExceptionHandler` | `application` |
| `adapter.out.crm` | `CrmHttpAdapter`, `CrmTranslator`, `CrmChaosAdapter` | `application`, `domain` |
| `adapter.out.persistence` | Entités et dépôts JPA, copies locales, données de démonstration | `application`, `domain` |
| `config` | Sécurité, assemblage des cas d'usage, horloge | tout |

Les cas d'usage ne sont pas des composants Spring : ils sont instanciés dans `ApplicationConfig`. Seule l'annotation `@Transactional` y apparaît, pour délimiter les écritures. Les scénarios Cucumber les exécutent avec les adaptateurs en mémoire de `InMemoryAdapters`, ce qui rend la suite métier rapide et indépendante de l'infrastructure.

## Lecture d'une fiche client

1. `CustomerService.view` vérifie que le client appartient au portefeuille du conseiller (le manager voit tout).
2. `ProfileLoader` interroge le CRM via le port `CrmGateway`.
   - Succès : la fiche est traduite en objets du domaine et enregistrée en copie locale (`customer_snapshot`).
   - Échec ou disjoncteur ouvert : la dernière copie locale est utilisée, marquée `live: false`. Sans copie, l'API répond 503 `CRM_UNAVAILABLE`.
3. Les échanges, tâches et recommandations écartées sont lus dans la base de l'application.
4. `NextBestActionEngine` applique chaque `RecommendationRule` à la situation du client, retire les recommandations écartées ou déjà suivies d'une tâche ouverte, puis trie par priorité.

## Résilience

| Mécanisme | Réglage |
|---|---|
| Délais | connexion 1 s, lecture 2 s |
| Nouvelle tentative | 2 essais au total, à 200 ms d'intervalle, sur erreur réseau ou 5xx |
| Disjoncteur | fenêtre de 6 appels, ouverture à 50 % d'échecs (3 appels minimum), réouverture partielle après 15 s |
| Repli | copie locale de la dernière fiche lue |

Une erreur 404 du CRM n'est pas une panne : elle signifie « client inconnu » et ne compte pas dans le disjoncteur.

Le manager peut simuler une panne (`PUT /api/crm/outage`) : `CrmChaosAdapter` fait échouer les appels sans toucher au CRM, puis réarme le disjoncteur quand la panne est levée.

## Modèle de données (application)

| Table | Rôle |
|---|---|
| `app_user` | Conseillers et managers |
| `portfolio_assignment` | Conseiller attitré de chaque client |
| `interaction` | Échanges tracés |
| `follow_up_task` | Tâches, avec la recommandation d'origine le cas échéant |
| `recommendation_dismissal` | Recommandations écartées, motif et date de fin |
| `customer_snapshot` | Dernière fiche lue dans le CRM (JSON) et texte de recherche |

Le schéma est géré par Flyway (`db/migration`).

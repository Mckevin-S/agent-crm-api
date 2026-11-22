# Contribuer à Agent CRM API

Ce document décrit la façon de travailler sur le projet : branches, commits, revue et qualité.

## Branches (Git Flow simplifié)

| Branche | Rôle | Règle |
|---|---|---|
| `main` | Code livré, chaque version est taguée (`v1.0.0`) | Jamais de commit direct ; uniquement des merges depuis `release/*` ou `hotfix/*` |
| `develop` | Intégration de la prochaine version | Reçoit les branches de travail après revue et CI verte |
| `feature/ACR-<n>-<sujet>` | Une fonctionnalité | Créée depuis `develop`, fusionnée dans `develop` |
| `fix/<sujet>` | Une correction sur `develop` | Idem |
| `chore/<sujet>`, `docs/<sujet>` | Outillage, configuration, documentation | Idem |
| `release/<version>` | Stabilisation d'une version | Créée depuis `develop`, fusionnée dans `main` (tag) puis dans `develop` |
| `hotfix/<version>` | Correction urgente en production | Créée depuis `main`, fusionnée dans `main` et `develop` |

Les noms de branches sont en minuscules, avec des tirets : `feature/ACR-4-adaptateurs`, `fix/initiales-client`.

Les fusions se font sans avance rapide (`--no-ff`) pour garder la trace de chaque branche dans l'historique.

## Messages de commit (Conventional Commits)

```
<type>(<portée>): <description à l'impératif, en minuscules>

<corps facultatif : le pourquoi, pas le comment>
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction d'anomalie |
| `refactor` | Restructuration sans changement de comportement |
| `test` | Ajout ou correction de tests |
| `docs` | Documentation |
| `build` | Dépendances, Maven, npm, Docker |
| `ci` | Intégration continue |
| `chore` | Maintenance diverse |

Portées utilisées : `domaine`, `recommandations`, `suivi`, `crm`, `api`, `auth`, `db`, `demo`, `front`, `e2e`.

Exemples :

```
feat(recommandations): relance des sinistres ouverts depuis plus de 30 jours
fix(crm): réarme le disjoncteur à la fin d'une panne simulée
test(suivi): une recommandation écartée exige un motif
```

## Pull requests

1. Une PR = un sujet. Elle cible `develop`.
2. La CI doit être verte : build, tests unitaires, scénarios Cucumber, règles d'architecture, tests d'intégration, lint, tests de bout en bout.
3. Le modèle de PR (`.github/pull_request_template.md`) sert de liste de contrôle.
4. Toute nouvelle règle métier arrive avec son scénario Gherkin ou son test unitaire.

## Qualité du code

### Back-end

- Architecture hexagonale : `domain` (Java pur), `application` (cas d'usage et ports), `adapter` (web, persistance, CRM), `config` (assemblage). Les règles sont vérifiées par `HexagonalArchitectureTest`.
- Le domaine ne connaît ni Spring, ni JPA, ni le CRM. Les objets métier sont des records immuables ou des classes à invariants (`FollowUpTask`).
- Une nouvelle règle de recommandation implémente `RecommendationRule` et s'ajoute au moteur dans `ApplicationConfig`.
- Le vocabulaire du CRM ne sort pas de `adapter.out.crm` : toute nouvelle donnée passe par `CrmTranslator`.
- Erreurs métier : `BusinessRuleViolation` avec un code stable (`DUE_DATE_IN_PAST`...), traduites en réponses RFC 7807.
- Tests d'intégration sur une vraie base PostgreSQL (Testcontainers), suffixe `IT`.

### Front-end

- Composants autonomes (`standalone`), signaux, `ChangeDetectionStrategy.OnPush`.
- Un seul client HTTP typé (`ApiService`) ; les actions de suivi partagées passent par `FollowUpActions`.
- L'état du CRM (`CrmStatusService`) est la seule source pour l'affichage du mode dégradé.
- Les droits affichés reprennent ceux de l'API, mais la vérification reste côté serveur.
- `npm run lint` doit passer sans avertissement.

## Commandes utiles

```bash
# CRM simulé
cd crm-simulator && mvn verify

# Back-end
cd backend && mvn verify                 # unitaires, Cucumber, ArchUnit, intégration (Docker requis)
cd backend && mvn test -Dtest=CucumberTest   # scénarios métier seuls

# Front-end
cd frontend && npm run lint && npm run test:ci && npm run build

# Bout en bout
docker compose up -d --build && cd frontend && npm run e2e
```
